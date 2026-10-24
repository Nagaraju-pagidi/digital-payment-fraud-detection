package com.fraudpay.payment.web;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientInfo {

    private ClientInfo() {
    }

    /** Real client IP (nginx and the gateway add X-Forwarded-For). */
    public static String ip(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        String real = request.getHeader("X-Real-IP");
        return real != null ? real : request.getRemoteAddr();
    }
}
