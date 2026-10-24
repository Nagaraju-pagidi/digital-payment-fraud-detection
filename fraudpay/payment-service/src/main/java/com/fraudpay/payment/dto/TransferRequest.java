package com.fraudpay.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Net-banking fund transfer to a saved payee. Step 1 (PIN) - step 2 is the email OTP. */
public record TransferRequest(
        @NotNull(message = "Choose a payee") Long payeeId,
        @NotNull(message = "Enter amount") @DecimalMin(value = "1.00", message = "Minimum amount is Rs 1") BigDecimal amount,
        @NotBlank(message = "Choose IMPS or NEFT") String mode,
        @Size(max = 140) String remarks,
        @NotBlank(message = "Enter transaction PIN") String pin,
        @NotBlank String deviceId,
        String deviceLabel,
        Double latitude,
        Double longitude
) {
}
