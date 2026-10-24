package com.fraudpay.payment.event;

/** Logical event labels used by the transactional outbox (not Kafka topics). */
public final class Topics {
    public static final String PAYMENT_REQUESTED = "payment.requested";   // FraudPay  -> FraudShield
    public static final String FRAUD_DECISIONS = "fraud.decisions";       // FraudShield -> FraudPay
    public static final String ACCOUNT_COMMANDS = "account.commands";     // FraudShield -> FraudPay (freeze/unfreeze)

    private Topics() {
    }
}
