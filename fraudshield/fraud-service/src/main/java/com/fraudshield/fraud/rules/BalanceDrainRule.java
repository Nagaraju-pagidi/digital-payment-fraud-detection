package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** R06 - The payment empties most of the wallet (typical when a fraudster takes over an account). */
@Component
public class BalanceDrainRule implements FraudRule {
    public String id() { return "R06"; }
    public String name() { return "Balance drain"; }
    public String description() { return "Payment uses 80% or more of the available balance"; }
    public int maxPoints() { return 25; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        BigDecimal before = ctx.event().senderBalanceBefore();
        BigDecimal amount = ctx.event().amount();
        if (before == null || before.signum() <= 0 || amount.compareTo(BigDecimal.valueOf(5000)) < 0) {
            return Optional.empty();
        }
        BigDecimal share = amount.multiply(BigDecimal.valueOf(100)).divide(before, 0, RoundingMode.HALF_UP);
        if (share.compareTo(BigDecimal.valueOf(80)) < 0) return Optional.empty();
        return hit(25, "Sends " + share + "% of the available balance (Rs " + Geo.inr(amount)
                + " of Rs " + Geo.inr(before) + ")");
    }
}
