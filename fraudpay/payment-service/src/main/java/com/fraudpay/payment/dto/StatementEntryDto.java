package com.fraudpay.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementEntryDto(Long id, Instant date, String description, String referenceNo, String type,
                                BigDecimal debit, BigDecimal credit, BigDecimal balance) {
}
