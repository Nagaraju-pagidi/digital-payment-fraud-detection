package com.fraudpay.payment.dto;

import jakarta.validation.constraints.NotBlank;

/** userId can be the User ID or the Customer ID. Device + location are captured automatically. */
public record LoginRequest(
        @NotBlank(message = "Enter User ID") String userId,
        @NotBlank(message = "Enter password") String password,
        @NotBlank String deviceId,
        String deviceLabel,
        Double latitude,
        Double longitude
) {
}
