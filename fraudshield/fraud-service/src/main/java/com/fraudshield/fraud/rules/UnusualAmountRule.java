package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * R05 - Amount is unusual FOR THIS CUSTOMER (not a fixed limit for everyone).
 * Compared with the customer's own average of approved payments.
 */
@Component
public class UnusualAmountRule implements FraudRule {
    public String id() { return "R05"; }
    public String name() { return "Unusual amount"; }
    public String description() { return "Amount is 3x (or 10x) higher than this customer's own average payment"; }
    public int maxPoints() { return 45; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        BigDecimal amount = ctx.event().amount();
        var p = ctx.profile();
        if (p == null || p.approvedCount() < 3) {
            // no reliable history yet: only very large amounts are suspicious
            if (amount.compareTo(BigDecimal.valueOf(25000)) >= 0) {
                return hit(20, "Rs " + Geo.inr(amount) + " with almost no payment history to compare");
            }
            return Optional.empty();
        }
        BigDecimal avg = p.average();
        if (avg.signum() == 0 || amount.compareTo(BigDecimal.valueOf(5000)) < 0) return Optional.empty();
        BigDecimal ratio = amount.divide(avg, 1, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.TEN) >= 0) {
            return hit(45, "Rs " + Geo.inr(amount) + " is " + ratio + "x this customer's usual Rs " + Geo.inr(avg));
        }
        if (ratio.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return hit(30, "Rs " + Geo.inr(amount) + " is " + ratio + "x this customer's usual Rs " + Geo.inr(avg));
        }
        return Optional.empty();
    }
}
