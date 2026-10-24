package com.fraudpay.payment.event;

public record RuleHit(String ruleId, String name, int points, String detail) {
}
