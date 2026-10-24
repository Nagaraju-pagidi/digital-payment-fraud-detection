-- ===================== FraudShield: fraud-service database =====================

-- Fraud analysts who log into the Control Center
CREATE TABLE analysts (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- One row per payment checked by the rule engine
CREATE TABLE fraud_cases (
    transaction_id        BIGINT PRIMARY KEY,
    channel               VARCHAR(10),                 -- IMPS | NEFT | UPI
    sender_id             BIGINT         NOT NULL,
    sender_name           VARCHAR(100)   NOT NULL,
    sender_email          VARCHAR(120),
    sender_upi            VARCHAR(60)    NOT NULL,
    receiver_name         VARCHAR(100),
    receiver_upi          VARCHAR(60)    NOT NULL,
    amount                NUMERIC(14, 2) NOT NULL,
    note                  VARCHAR(140),
    device_id             VARCHAR(64),
    device_label          VARCHAR(100),
    new_device            BOOLEAN        NOT NULL DEFAULT FALSE,
    latitude              DOUBLE PRECISION,
    longitude             DOUBLE PRECISION,
    location_city         VARCHAR(80),
    location_country      VARCHAR(2),
    home_city             VARCHAR(60),
    home_country          VARCHAR(2),
    distance_from_home_km DOUBLE PRECISION,
    score                 INT            NOT NULL,
    decision              VARCHAR(20)    NOT NULL,  -- automatic verdict: APPROVED | HOLD | BLOCKED
    status                VARCHAR(20)    NOT NULL,  -- APPROVED | HOLD | BLOCKED | ANALYST_APPROVED | ANALYST_REJECTED
    rule_hits             TEXT           NOT NULL,  -- JSON list of triggered rules
    simulated             BOOLEAN        NOT NULL DEFAULT FALSE,
    account_frozen        BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ    NOT NULL,
    decided_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    reviewed_by           VARCHAR(100),
    review_note           VARCHAR(255),
    reviewed_at           TIMESTAMPTZ
);
CREATE INDEX idx_cases_sender_time ON fraud_cases (sender_id, created_at DESC);
CREATE INDEX idx_cases_status ON fraud_cases (status);
CREATE INDEX idx_cases_created ON fraud_cases (created_at DESC);

-- Each customer's NORMAL behaviour, learned from approved payments
CREATE TABLE user_profiles (
    user_id         BIGINT PRIMARY KEY,
    approved_count  INT            NOT NULL DEFAULT 0,
    approved_total  NUMERIC(16, 2) NOT NULL DEFAULT 0,
    night_count     INT            NOT NULL DEFAULT 0,
    last_payment_at TIMESTAMPTZ,
    last_lat        DOUBLE PRECISION,
    last_lon        DOUBLE PRECISION,
    last_city       VARCHAR(80),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Devices a customer has successfully paid from
CREATE TABLE known_devices (
    user_id    BIGINT      NOT NULL,
    device_id  VARCHAR(64) NOT NULL,
    first_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, device_id)
);

-- People a customer has successfully paid before
CREATE TABLE known_receivers (
    user_id      BIGINT      NOT NULL,
    receiver_upi VARCHAR(60) NOT NULL,
    first_paid   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, receiver_upi)
);

-- Frozen accounts
CREATE TABLE account_flags (
    user_id     BIGINT PRIMARY KEY,
    user_name   VARCHAR(100),
    user_email  VARCHAR(120),
    user_upi    VARCHAR(60),
    frozen      BOOLEAN      NOT NULL DEFAULT FALSE,
    reason      VARCHAR(255),
    frozen_at   TIMESTAMPTZ,
    unfrozen_at TIMESTAMPTZ,
    unfrozen_by VARCHAR(100)
);

-- Idempotent consumer
CREATE TABLE processed_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
