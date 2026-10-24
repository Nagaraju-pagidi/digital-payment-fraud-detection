package com.fraudpay.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** FraudShield's verdict for a payment: APPROVED, HOLD or BLOCKED. */
public record FraudDecisionEvent(
        String eventId,
        Long transactionId,
        Long senderId,
        String senderName,
        String senderEmail,
        String senderUpi,
        String receiverUpi,
        String receiverName,
        BigDecimal amount,
        int score,
        String decision,      // APPROVED | HOLD | BLOCKED
        List<String> reasons,
        List<RuleHit> ruleHits,
        String source,        // AUTO (rule engine) | ANALYST (human review)
        String analystNote,
        boolean simulated,
        Instant decidedAt
) {
}
