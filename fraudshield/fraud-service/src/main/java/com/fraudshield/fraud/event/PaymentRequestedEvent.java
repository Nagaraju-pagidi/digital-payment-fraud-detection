package com.fraudshield.fraud.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sent to FraudShield for every new payment.
 * It carries everything the fraud engine needs (event-carried state transfer),
 * so FraudShield never has to read FraudPay's database.
 */
public record PaymentRequestedEvent(
        String eventId,
        Long transactionId,
        // who is paying + their registered profile
        Long senderId,
        String senderName,
        String senderEmail,
        String senderUpi,
        String senderHomeCity,
        String senderHomeCountry,
        Double senderHomeLat,
        Double senderHomeLon,
        Instant senderAccountCreatedAt,
        String senderRegisteredDeviceId,
        java.math.BigDecimal senderBalanceBefore,
        // who receives
        Long receiverId,
        String receiverName,
        String receiverUpi,
        // the payment itself + context captured automatically by the app
        BigDecimal amount,
        String note,
        String deviceId,
        String deviceLabel,
        Double latitude,
        Double longitude,
        String locationCity,
        String locationCountry,
        boolean simulated,
        Instant createdAt,
        // net banking context
        String channel,          // IMPS | NEFT | UPI
        Instant payeeAddedAt     // when the payee was added (null for UPI)
) {
}
