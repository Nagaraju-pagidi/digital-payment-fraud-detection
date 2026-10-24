package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * R12 - Net banking: big transfer to a payee added in the last 24 hours.
 * Classic fraud pattern: the fraudster logs in, adds his own account as a payee and drains the victim.
 */
@Component
public class NewPayeeRule implements FraudRule {
    public String id() { return "R12"; }
    public String name() { return "New payee, big transfer"; }
    public String description() { return "IMPS/NEFT of Rs 10,000 or more to a payee added less than 24 hours ago"; }
    public int maxPoints() { return 25; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        var e = ctx.event();
        if (e.payeeAddedAt() == null || "UPI".equals(e.channel())) return Optional.empty();
        if (e.amount().compareTo(BigDecimal.valueOf(10000)) < 0) return Optional.empty();
        Duration age = Duration.between(e.payeeAddedAt(), e.createdAt());
        if (age.toHours() >= 24) return Optional.empty();
        String when = age.toMinutes() < 60 ? age.toMinutes() + " minute(s)" : age.toHours() + " hour(s)";
        return hit(25, "Rs " + Geo.inr(e.amount()) + " " + e.channel() + " to a payee added only " + when + " ago");
    }
}
