package com.fraudshield.fraud.dto;

import com.fraudshield.fraud.event.RuleHit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CaseDto(
        Long transactionId,
        String channel,
        Long senderId,
        String senderName,
        String senderUpi,
        String receiverName,
        String receiverUpi,
        BigDecimal amount,
        String note,
        String deviceLabel,
        boolean newDevice,
        Double latitude,
        Double longitude,
        String locationCity,
        String locationCountry,
        String homeCity,
        String homeCountry,
        Double distanceFromHomeKm,
        int score,
        String decision,
        String status,
        List<RuleHit> ruleHits,
        boolean simulated,
        boolean accountFrozen,
        Instant createdAt,
        String reviewedBy,
        String reviewNote,
        Instant reviewedAt
) {
}
