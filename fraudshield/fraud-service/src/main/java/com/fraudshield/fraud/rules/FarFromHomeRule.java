package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** R02 - Same country, but more than 500 km from the customer's home city. */
@Component
public class FarFromHomeRule implements FraudRule {
    public String id() { return "R02"; }
    public String name() { return "Far from home"; }
    public String description() { return "Payment made more than 500 km away from the registered home city"; }
    public int maxPoints() { return 30; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        if (ctx.isForeign() || !ctx.isFarFromHome()) return Optional.empty();
        return hit(30, "Payment made " + Geo.inr(ctx.distanceFromHomeKm()) + " km away from home city "
                + ctx.event().senderHomeCity() + " (at " + ctx.event().locationCity() + ")");
    }
}
