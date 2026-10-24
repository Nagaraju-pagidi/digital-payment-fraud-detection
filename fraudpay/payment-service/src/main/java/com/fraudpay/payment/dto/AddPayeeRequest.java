package com.fraudpay.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddPayeeRequest(
        @NotBlank(message = "Enter account number") String accountNumber,
        @NotBlank(message = "Re-enter account number") String confirmAccountNumber,
        @NotBlank(message = "Enter IFSC") String ifsc,
        @Size(max = 40) String nickname
) {
}
