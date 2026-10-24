package com.fraudshield.fraud.event;

public record RuleHit(String ruleId, String name, int points, String detail) {
}
