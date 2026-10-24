package com.fraudpay.payment.security;

import com.fraudpay.payment.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues the session token (JWT, 30 minutes) for net-banking customers.
 * The API Gateway verifies it with the same secret on every request.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.expiry-minutes:30}") long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = expiryMinutes;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", "CUSTOMER")
                .claim("name", user.getFullName())
                .claim("customerId", user.getCustomerId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expiryMinutes))))
                .signWith(key)
                .compact();
    }
}
