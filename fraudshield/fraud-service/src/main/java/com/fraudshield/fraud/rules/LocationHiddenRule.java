package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** R11 - The app could not get the location (permission denied / blocked). Small risk on its own. */
@Component
public class LocationHiddenRule implements FraudRule {
    public String id() { return "R11"; }
    public String name() { return "Location hidden"; }
    public String description() { return "Location permission was denied, so the place of payment cannot be verified"; }
    public int maxPoints() { return 10; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        if (ctx.hasLocation()) return Optional.empty();
        return hit(10, "Location not shared - place of payment could not be verified");
    }
}
