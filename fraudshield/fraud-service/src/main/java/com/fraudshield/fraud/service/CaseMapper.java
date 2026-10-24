package com.fraudshield.fraud.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudshield.fraud.domain.AccountFlag;
import com.fraudshield.fraud.domain.FraudCase;
import com.fraudshield.fraud.dto.AccountFlagDto;
import com.fraudshield.fraud.dto.CaseDto;
import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CaseMapper {

    private static final TypeReference<List<RuleHit>> HITS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public CaseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<RuleHit> hits(FraudCase c) {
        try {
            return objectMapper.readValue(c.getRuleHits(), HITS);
        } catch (Exception e) {
            return List.of();
        }
    }

    public String hitsJson(List<RuleHit> hits) {
        try {
            return objectMapper.writeValueAsString(hits);
        } catch (Exception e) {
            return "[]";
        }
    }

    public CaseDto toDto(FraudCase c) {
        return new CaseDto(c.getTransactionId(), c.getChannel(), c.getSenderId(), c.getSenderName(), c.getSenderUpi(),
                c.getReceiverName(), c.getReceiverUpi(), c.getAmount(), c.getNote(), c.getDeviceLabel(),
                c.isNewDevice(), c.getLatitude(), c.getLongitude(), c.getLocationCity(), c.getLocationCountry(),
                c.getHomeCity(), c.getHomeCountry(), c.getDistanceFromHomeKm(), c.getScore(), c.getDecision(),
                c.getStatus().name(), hits(c), c.isSimulated(), c.isAccountFrozen(), c.getCreatedAt(),
                c.getReviewedBy(), c.getReviewNote(), c.getReviewedAt());
    }

    public AccountFlagDto toDto(AccountFlag f) {
        return new AccountFlagDto(f.getUserId(), f.getUserName(), f.getUserUpi(), f.getUserEmail(), f.isFrozen(),
                f.getReason(), f.getFrozenAt(), f.getUnfrozenAt(), f.getUnfrozenBy());
    }
}
