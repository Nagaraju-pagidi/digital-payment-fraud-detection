-- ===================== NexaPay: payment-service database =====================

-- Customers = KYC profile + savings/current account + wallet balance + security state
CREATE TABLE users (
    id                   BIGSERIAL PRIMARY KEY,
    customer_id          VARCHAR(12)    NOT NULL UNIQUE,     -- FP1234567
    username             VARCHAR(30)    NOT NULL UNIQUE,     -- net-banking login id
    full_name            VARCHAR(100)   NOT NULL,
    date_of_birth        DATE           NOT NULL,
    gender               VARCHAR(10)    NOT NULL,
    email                VARCHAR(120)   NOT NULL UNIQUE,
    phone                VARCHAR(10)    NOT NULL UNIQUE,
    pan                  VARCHAR(10)    NOT NULL UNIQUE,
    aadhaar_hash         VARCHAR(64)    NOT NULL UNIQUE,     -- SHA-256, the number itself is never stored
    aadhaar_last4        VARCHAR(4)     NOT NULL,
    address_line         VARCHAR(200)   NOT NULL,
    city                 VARCHAR(60)    NOT NULL,
    state                VARCHAR(60)    NOT NULL,
    pincode              VARCHAR(6)     NOT NULL,
    occupation           VARCHAR(40)    NOT NULL,
    annual_income        VARCHAR(30)    NOT NULL,
    nominee_name         VARCHAR(100),
    nominee_relation     VARCHAR(30),
    account_type         VARCHAR(20)    NOT NULL,
    account_number       VARCHAR(12)    NOT NULL UNIQUE,
    ifsc                 VARCHAR(11)    NOT NULL,
    branch               VARCHAR(60)    NOT NULL,
    upi_id               VARCHAR(60)    NOT NULL UNIQUE,
    password_hash        VARCHAR(100)   NOT NULL,
    pin_hash             VARCHAR(100)   NOT NULL,             -- 6-digit transaction PIN (BCrypt)
    balance              NUMERIC(14, 2) NOT NULL DEFAULT 0,
    status               VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | FROZEN
    status_reason        VARCHAR(255),
    -- "home" for fraud checks = where the customer registered (GPS), city coordinates as fallback
    home_city            VARCHAR(80),
    home_country         VARCHAR(2),
    home_lat             DOUBLE PRECISION,
    home_lon             DOUBLE PRECISION,
    registered_device_id VARCHAR(64),
    failed_logins        INT            NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ,
    pin_failures         INT            NOT NULL DEFAULT 0,
    pin_locked_until     TIMESTAMPTZ,
    last_login_at        TIMESTAMPTZ,
    bot                  BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Devices the customer verified (first device at registration, others with an email OTP)
CREATE TABLE trusted_devices (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    device_id    VARCHAR(64)  NOT NULL,
    device_label VARCHAR(100),
    added_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ,
    UNIQUE (user_id, device_id)
);

-- Every login attempt (shown to the customer in "Profile & Security")
CREATE TABLE login_history (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    success      BOOLEAN      NOT NULL,
    detail       VARCHAR(120),
    device_id    VARCHAR(64),
    device_label VARCHAR(100),
    ip_address   VARCHAR(64),
    location     VARCHAR(80),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_login_history_user ON login_history (user_id, created_at DESC);

-- One-time passwords sent by email (registration, new-device login, add payee, fund transfer)
CREATE TABLE otp_challenges (
    id         UUID PRIMARY KEY,
    user_id    BIGINT,
    purpose    VARCHAR(20)  NOT NULL,
    email      VARCHAR(120) NOT NULL,
    code_hash  VARCHAR(64)  NOT NULL,
    payload    TEXT,
    attempts   INT          NOT NULL DEFAULT 0,
    resends    INT          NOT NULL DEFAULT 0,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Payees (beneficiaries) for net-banking transfers
CREATE TABLE beneficiaries (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id),
    payee_user_id  BIGINT       NOT NULL REFERENCES users (id),
    name           VARCHAR(100) NOT NULL,
    nickname       VARCHAR(40),
    account_number VARCHAR(12)  NOT NULL,
    ifsc           VARCHAR(11)  NOT NULL,
    active_from    TIMESTAMPTZ  NOT NULL,       -- cooling period after adding a payee
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX ux_beneficiary_active ON beneficiaries (user_id, account_number) WHERE deleted = FALSE;

-- Money transfers (IMPS / NEFT / UPI)
CREATE TABLE payments (
    id               BIGSERIAL PRIMARY KEY,
    reference_no     VARCHAR(20)    NOT NULL UNIQUE,
    channel          VARCHAR(10)    NOT NULL,     -- IMPS | NEFT | UPI
    sender_id        BIGINT         NOT NULL REFERENCES users (id),
    sender_name      VARCHAR(100)   NOT NULL,
    sender_upi       VARCHAR(60)    NOT NULL,
    receiver_id      BIGINT         NOT NULL REFERENCES users (id),
    receiver_name    VARCHAR(100)   NOT NULL,
    receiver_upi     VARCHAR(60)    NOT NULL,
    receiver_account VARCHAR(12)    NOT NULL,
    payee_added_at   TIMESTAMPTZ,
    amount           NUMERIC(14, 2) NOT NULL,
    note             VARCHAR(140),
    -- PROCESSING -> SUCCESS | ON_HOLD | BLOCKED ;  ON_HOLD -> SUCCESS | REJECTED
    status           VARCHAR(20)    NOT NULL,
    status_message   VARCHAR(255),
    fraud_score      INT,
    fraud_reasons    TEXT,
    device_id        VARCHAR(64),
    device_label     VARCHAR(100),
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    location_city    VARCHAR(80),
    location_country VARCHAR(2),
    simulated        BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);
CREATE INDEX idx_payments_sender ON payments (sender_id, created_at DESC);
CREATE INDEX idx_payments_receiver ON payments (receiver_id, created_at DESC);
CREATE INDEX idx_payments_status ON payments (status, created_at);

-- Account statement (passbook): every debit / credit / refund with the running balance
CREATE TABLE ledger_entries (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT         NOT NULL REFERENCES users (id),
    payment_id    BIGINT,
    entry_type    VARCHAR(10)    NOT NULL,   -- OPENING | DEBIT | CREDIT | REFUND
    amount        NUMERIC(14, 2) NOT NULL,
    balance_after NUMERIC(14, 2) NOT NULL,
    description   VARCHAR(200)   NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ledger_user ON ledger_entries (user_id, created_at DESC, id DESC);

-- Transactional Outbox (payment + event in ONE database transaction)
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    message_key    VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at   TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published = FALSE;

-- Idempotent consumer
CREATE TABLE processed_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
