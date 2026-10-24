package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** R08 - Too many payments in a short time (card-testing / panic draining pattern). */
@Component
public class VelocityRule implements FraudRule {
    public String id() { return "R08"; }
    public String name() { return "Sudden burst"; }
    public String description() { return "5 or more payments within 10 minutes (8 or more = very high risk)"; }
    public int maxPoints() { return 70; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        // Simulator bots pay at machine speed on purpose; they stand for many different people.
        if (ctx.event().simulated()) return Optional.empty();
        long total = ctx.recentPayments() + 1; // including this one
        if (total >= 8) return hit(70, total + " payments within 10 minutes");
        if (total >= 5) return hit(40, total + " payments within 10 minutes");
        return Optional.empty();
    }
}
