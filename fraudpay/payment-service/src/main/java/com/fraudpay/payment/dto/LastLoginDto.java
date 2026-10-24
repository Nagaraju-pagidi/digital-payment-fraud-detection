package com.fraudpay.payment.dto;

import java.time.Instant;

public record LastLoginDto(Instant at, String deviceLabel, String location, String ipAddress) {
}
