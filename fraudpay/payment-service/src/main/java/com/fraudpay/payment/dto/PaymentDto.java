package com.fraudpay.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A payment as seen by ONE customer (direction = SENT or RECEIVED). */
public record PaymentDto(
        Long id,
        String referenceNo,
        String channel,
        String direction,
        String counterpartyName,
        String counterpartyUpi,
        String counterpartyAccount,
        BigDecimal amount,
        String note,
        String status,
        String statusMessage,
        Integer fraudScore,
        List<String> reasons,
        String locationCity,
        String locationCountry,
        String deviceLabel,
        Instant createdAt,
        Instant updatedAt
) {
}
