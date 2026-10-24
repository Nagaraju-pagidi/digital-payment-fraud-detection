package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** R01 - The payment comes from a different country than the customer's registered country. */
@Component
public class ForeignCountryRule implements FraudRule {
    public String id() { return "R01"; }
    public String name() { return "Foreign country"; }
    public String description() { return "Payment made from a country different from the customer's registered home country"; }
    public int maxPoints() { return 50; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        if (!ctx.isForeign()) return Optional.empty();
        var e = ctx.event();
        return hit(50, "Payment from " + e.locationCity() + " (" + e.locationCountry() + ") but the customer is registered in "
                + e.senderHomeCity() + " (" + e.senderHomeCountry() + ")");
    }
}
