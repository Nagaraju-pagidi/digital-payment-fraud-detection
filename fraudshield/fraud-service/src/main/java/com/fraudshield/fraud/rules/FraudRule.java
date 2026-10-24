package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;

import java.util.Optional;

/**
 * One fraud rule (Strategy pattern).
 * Each rule looks at the context and either adds risk points with a human-readable
 * explanation, or returns empty. Adding a new rule = adding one new @Component class.
 */
public interface FraudRule {

    String id();

    String name();

    /** Short description shown in the Control Center "Rule book". */
    String description();

    /** Maximum points this rule can add (for the rule book). */
    int maxPoints();

    Optional<RuleHit> evaluate(FraudContext ctx);

    default Optional<RuleHit> hit(int points, String detail) {
        return Optional.of(new RuleHit(id(), name(), points, detail));
    }
}
