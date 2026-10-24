package com.fraudpay.payment.dto;

import java.time.Instant;

public record PayeeDto(Long id, String name, String nickname, String accountNumber, String ifsc, String branch,
                       boolean active, Instant activeFrom, Instant addedAt) {
}
