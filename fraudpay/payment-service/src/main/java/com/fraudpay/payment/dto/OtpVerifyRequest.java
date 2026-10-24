package com.fraudpay.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank String challengeId,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "OTP must be 6 digits") String otp,
        boolean trustDevice
) {
}
