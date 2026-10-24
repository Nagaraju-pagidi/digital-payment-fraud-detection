package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** R04 - Payment from a phone/laptop this customer has never used before. */
@Component
public class NewDeviceRule implements FraudRule {
    public String id() { return "R04"; }
    public String name() { return "New device"; }
    public String description() { return "Payment from a device never used by this customer before"; }
    public int maxPoints() { return 30; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        if (!ctx.isNewDevice()) return Optional.empty();
        String label = ctx.event().deviceLabel() == null ? "unknown device" : ctx.event().deviceLabel();
        return hit(30, "First payment from this device (" + label + ")");
    }
}
