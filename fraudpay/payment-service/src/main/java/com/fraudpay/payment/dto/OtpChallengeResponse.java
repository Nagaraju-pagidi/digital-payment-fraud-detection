package com.fraudpay.payment.dto;

/** "We sent an OTP to r***i@demo.in, valid for 300 seconds". */
public record OtpChallengeResponse(String challengeId, String maskedEmail, long expiresInSeconds, String purpose) {
}
