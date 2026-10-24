package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** R09 - Payment between midnight and 5 AM by someone who never pays at night. */
@Component
public class NightTimeRule implements FraudRule {
    public String id() { return "R09"; }
    public String name() { return "Unusual time"; }
    public String description() { return "Payment between 12 AM and 5 AM by a customer who normally never pays at night"; }
    public int maxPoints() { return 15; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        int hour = ctx.localTime().getHour();
        if (hour >= 5) return Optional.empty();
        if (ctx.profile() != null && ctx.profile().nightCount() > 0) return Optional.empty();
        return hit(15, String.format("Payment at %02d:%02d - this customer has never paid at night before",
                hour, ctx.localTime().getMinute()));
    }
}
