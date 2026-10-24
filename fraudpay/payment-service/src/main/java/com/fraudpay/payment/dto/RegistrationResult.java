package com.fraudpay.payment.dto;

public record RegistrationResult(String customerId, String username, String fullName, String accountNumber,
                                 String ifsc, String branch, String upiId, String accountType,
                                 java.math.BigDecimal openingBalance) {
}
