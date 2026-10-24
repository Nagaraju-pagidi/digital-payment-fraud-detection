package com.fraudpay.payment.dto;

import java.time.Instant;

public record LoginHistoryDto(Instant at, boolean success, String detail, String deviceLabel, String ipAddress,
                              String location) {
}
