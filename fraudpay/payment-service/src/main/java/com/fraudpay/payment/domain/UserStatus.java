package com.fraudpay.payment.domain;

public enum UserStatus {
    ACTIVE,
    FROZEN   // frozen by FraudShield after repeated fraud attempts
}
