package com.fraudpay.payment.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Account opening form (all fields are re-validated by KycValidator). */
public record RegisterRequest(
        // personal
        @NotBlank(message = "Full name is required") String fullName,
        @NotNull(message = "Date of birth is required") LocalDate dateOfBirth,
        @NotBlank(message = "Select gender") String gender,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
        @NotBlank(message = "Mobile number is required") String phone,
        // KYC
        @NotBlank(message = "PAN is required") String pan,
        @NotBlank(message = "Aadhaar is required") String aadhaar,
        // address
        @NotBlank(message = "Address is required") @Size(max = 200) String addressLine,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "PIN code is required") String pincode,
        // account
        @NotBlank(message = "Select occupation") String occupation,
        @NotBlank(message = "Select annual income") String annualIncome,
        @NotBlank(message = "Select account type") String accountType,
        String nomineeName,
        String nomineeRelation,
        // net banking credentials
        @NotBlank(message = "Choose a User ID") String username,
        @NotBlank(message = "Choose a password") String password,
        @NotBlank(message = "Confirm the password") String confirmPassword,
        @NotBlank(message = "Choose a transaction PIN") String pin,
        @NotBlank(message = "Confirm the transaction PIN") String confirmPin,
        @AssertTrue(message = "Please accept the terms and conditions") boolean acceptTerms,
        // captured automatically by the browser
        @NotBlank String deviceId,
        String deviceLabel,
        Double latitude,
        Double longitude
) {
}
