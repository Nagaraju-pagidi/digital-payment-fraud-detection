package com.fraudpay.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** What the dashboard shows about the logged-in customer. */
public record UserDto(
        Long id,
        String customerId,
        String username,
        String fullName,
        String email,
        String phone,
        String accountNumber,
        String ifsc,
        String branch,
        String accountType,
        String upiId,
        BigDecimal balance,
        String status,
        String statusReason,
        String homeCity,
        Instant lastLoginAt,
        Instant createdAt
) {
}
