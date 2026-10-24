package com.fraudpay.payment.dto;

import java.time.Instant;

public record DeviceDto(Long id, String deviceLabel, Instant addedAt, Instant lastUsedAt, boolean current,
                        boolean registration) {
}
