package com.fraudpay.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePinRequest(@NotBlank String currentPin, @NotBlank String newPin, @NotBlank String confirmPin) {
}
