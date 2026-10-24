package com.fraudshield.fraud.security;

import com.fraudshield.fraud.domain.Analyst;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues tokens with role ANALYST for the FraudShield Control Center. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryHours;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.expiry-hours:12}") long expiryHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryHours = expiryHours;
    }

    public String issueToken(Analyst analyst) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(analyst.getUsername())
                .claim("role", "ANALYST")
                .claim("name", analyst.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(expiryHours))))
                .signWith(key)
                .compact();
    }
}
