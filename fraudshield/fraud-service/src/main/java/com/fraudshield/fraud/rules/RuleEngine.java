package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Runs every FraudRule, adds up the points and decides:
 *   score <  40  -> APPROVED
 *   score 40-69  -> HOLD     (a human analyst decides)
 *   score >= 70  -> BLOCKED  (money refunded)
 */
@Component
public class RuleEngine {

    public record Result(int score, String decision, List<RuleHit> hits) {
    }

    private final List<FraudRule> rules;
    private final int holdThreshold;
    private final int blockThreshold;

    public RuleEngine(List<FraudRule> rules,
                      @Value("${fraudshield.thresholds.hold:40}") int holdThreshold,
                      @Value("${fraudshield.thresholds.block:70}") int blockThreshold) {
        this.rules = rules.stream().sorted(Comparator.comparing(FraudRule::id)).toList();
        this.holdThreshold = holdThreshold;
        this.blockThreshold = blockThreshold;
    }

    public Result evaluate(FraudContext ctx) {
        List<RuleHit> hits = rules.stream()
                .map(rule -> rule.evaluate(ctx))
                .flatMap(Optional::stream)
                .toList();
        int score = hits.stream().mapToInt(RuleHit::points).sum();
        String decision = score >= blockThreshold ? "BLOCKED" : score >= holdThreshold ? "HOLD" : "APPROVED";
        return new Result(score, decision, hits);
    }

    public List<FraudRule> rules() {
        return rules;
    }

    public int holdThreshold() {
        return holdThreshold;
    }

    public int blockThreshold() {
        return blockThreshold;
    }
}
