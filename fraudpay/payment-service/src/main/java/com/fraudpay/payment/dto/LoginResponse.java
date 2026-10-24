package com.fraudpay.payment.dto;

/**
 * Either a session token (trusted device) or an OTP challenge (new device).
 */
public record LoginResponse(boolean otpRequired, OtpChallengeResponse challenge, String token,
                            UserDto user, LastLoginDto lastLogin) {

    public static LoginResponse otp(OtpChallengeResponse challenge) {
        return new LoginResponse(true, challenge, null, null, null);
    }

    public static LoginResponse ok(String token, UserDto user, LastLoginDto lastLogin) {
        return new LoginResponse(false, null, token, user, lastLogin);
    }
}
