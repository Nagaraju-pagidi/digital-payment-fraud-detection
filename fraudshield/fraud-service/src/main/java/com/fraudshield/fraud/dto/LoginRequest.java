package com.fraudshield.fraud.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Enter username") String username,
                           @NotBlank(message = "Enter password") String password) {
}
