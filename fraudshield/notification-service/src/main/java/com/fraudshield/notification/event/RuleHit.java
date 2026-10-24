package com.fraudshield.notification.event;

public record RuleHit(String ruleId, String name, int points, String detail) {
}
