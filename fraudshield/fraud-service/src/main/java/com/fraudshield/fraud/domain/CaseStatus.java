package com.fraudshield.fraud.domain;

public enum CaseStatus {
    APPROVED,          // auto-approved by the rule engine
    HOLD,              // waiting for an analyst
    BLOCKED,           // auto-blocked by the rule engine (money refunded)
    ANALYST_APPROVED,  // analyst released the held payment
    ANALYST_REJECTED   // analyst rejected the held payment (money refunded)
}
