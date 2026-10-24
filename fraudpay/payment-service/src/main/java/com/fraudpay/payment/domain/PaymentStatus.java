package com.fraudpay.payment.domain;

/**
 * Life of a payment (the saga):
 * PROCESSING -> SUCCESS   (FraudShield approved, money moved)
 * PROCESSING -> ON_HOLD   (suspicious, waiting for an analyst)
 * PROCESSING -> BLOCKED   (fraud, money refunded automatically)
 * ON_HOLD    -> SUCCESS   (analyst approved)
 * ON_HOLD    -> REJECTED  (analyst rejected, money refunded)
 */
public enum PaymentStatus {
    PROCESSING, SUCCESS, ON_HOLD, BLOCKED, REJECTED;

    public boolean isOpen() {
        return this == PROCESSING || this == ON_HOLD;
    }
}
