package com.fraudpay.payment.event;

import java.time.Instant;

/** FraudShield asks FraudPay to FREEZE or UNFREEZE a customer's account. */
public record AccountCommandEvent(
        String eventId,
        Long userId,
        String userName,
        String userEmail,
        String action,   // FREEZE | UNFREEZE
        String reason,
        Instant createdAt
) {
}
