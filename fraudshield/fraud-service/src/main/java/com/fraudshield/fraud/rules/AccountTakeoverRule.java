package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * R10 - Classic account-takeover pattern: NEW device + UNUSUAL location + NEW receiver, all together.
 * Each alone can be innocent; together they strongly suggest someone else is using the account.
 */
@Component
public class AccountTakeoverRule implements FraudRule {
    public String id() { return "R10"; }
    public String name() { return "Account takeover pattern"; }
    public String description() { return "New device + far/foreign location + never-paid-before receiver at the same time"; }
    public int maxPoints() { return 40; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        boolean oddPlace = ctx.isForeign() || ctx.isFarFromHome();
        if (ctx.isNewDevice() && oddPlace && !ctx.knownReceiver()) {
            return hit(40, "New device + unusual location + first payment to " + ctx.event().receiverUpi());
        }
        return Optional.empty();
    }
}
