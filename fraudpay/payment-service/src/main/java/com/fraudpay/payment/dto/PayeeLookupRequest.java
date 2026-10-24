package com.fraudpay.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PayeeLookupRequest(@NotBlank(message = "Enter account number") String accountNumber,
                                 @NotBlank(message = "Enter IFSC") String ifsc) {
}
