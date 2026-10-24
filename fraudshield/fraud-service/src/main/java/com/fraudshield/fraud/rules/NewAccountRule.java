package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/** R07 - A very new account moving a big amount (common with mule / fake accounts). */
@Component
public class NewAccountRule implements FraudRule {
    public String id() { return "R07"; }
    public String name() { return "New account, big amount"; }
    public String description() { return "Account younger than 7 days sending Rs 10,000 or more"; }
    public int maxPoints() { return 25; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        var e = ctx.event();
        if (e.senderAccountCreatedAt() == null || e.amount().compareTo(BigDecimal.valueOf(10000)) < 0) {
            return Optional.empty();
        }
        long days = Duration.between(e.senderAccountCreatedAt(), e.createdAt()).toDays();
        if (days >= 7) return Optional.empty();
        return hit(25, "Account opened only " + days + " day(s) ago is sending Rs " + Geo.inr(e.amount()));
    }
}
