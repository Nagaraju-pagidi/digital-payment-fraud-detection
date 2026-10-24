package com.fraudshield.fraud.dto;

import java.time.Instant;

public record AccountFlagDto(Long userId, String userName, String userUpi, String userEmail, boolean frozen,
                             String reason, Instant frozenAt, Instant unfrozenAt, String unfrozenBy) {
}
