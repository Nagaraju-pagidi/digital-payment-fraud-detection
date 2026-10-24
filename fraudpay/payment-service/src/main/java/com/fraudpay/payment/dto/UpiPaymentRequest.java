package com.fraudpay.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpiPaymentRequest(
        @NotBlank(message = "Enter UPI ID") String upiId,
        @NotNull(message = "Enter amount") @DecimalMin(value = "1.00", message = "Minimum amount is Rs 1") BigDecimal amount,
        @Size(max = 140) String note,
        @NotBlank(message = "Enter transaction PIN") String pin,
        @NotBlank String deviceId,
        String deviceLabel,
        Double latitude,
        Double longitude
) {
}
