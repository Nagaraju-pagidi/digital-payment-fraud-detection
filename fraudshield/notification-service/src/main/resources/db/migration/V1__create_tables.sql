-- ===================== FraudShield: notification-service database =====================

-- Every alert we tried to send. FAILED rows are retried by NotificationRetryJob
-- (a database-backed retry queue); after 8 attempts they become DEAD.
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT,
    user_id         BIGINT,
    type            VARCHAR(40)  NOT NULL,   -- PAYMENT_HOLD | PAYMENT_BLOCKED | PAYMENT_RELEASED | ACCOUNT_FROZEN | ...
    recipient       VARCHAR(120) NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(10)  NOT NULL,   -- SENT | FAILED | DEAD
    attempts        INT          NOT NULL DEFAULT 0,
    last_error      VARCHAR(500),
    next_attempt_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ
);
CREATE INDEX idx_notifications_retry ON notifications (next_attempt_at) WHERE status = 'FAILED';
CREATE INDEX idx_notifications_created ON notifications (created_at DESC);

CREATE TABLE processed_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
