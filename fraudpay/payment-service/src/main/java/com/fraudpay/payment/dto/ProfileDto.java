package com.fraudpay.payment.dto;

import java.time.Instant;
import java.time.LocalDate;

/** KYC profile with sensitive values masked. */
public record ProfileDto(
        String customerId, String username, String fullName, LocalDate dateOfBirth, String gender,
        String email, String phone, String panMasked, String aadhaarMasked,
        String addressLine, String city, String state, String pincode,
        String occupation, String annualIncome, String nomineeName, String nomineeRelation,
        String accountType, String accountNumber, String ifsc, String branch, String upiId,
        String homeCity, Instant createdAt
) {
}
