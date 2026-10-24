package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** A one-time password sent by email. Only the SHA-256 hash of the code is stored. */
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    private UUID id;
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false)
    private String purpose;
    @Column(nullable = false)
    private String email;
    @Column(name = "code_hash", nullable = false)
    private String codeHash;
    /** JSON with what to do after a correct OTP (pending registration, payee, transfer...). */
    private String payload;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private int resends;
    @Column(nullable = false)
    private boolean used;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected OtpChallenge() {
    }

    public OtpChallenge(UUID id, Long userId, String purpose, String email, String codeHash, String payload,
                        Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.purpose = purpose;
        this.email = email;
        this.codeHash = codeHash;
        this.payload = payload;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPurpose() { return purpose; }
    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public String getPayload() { return payload; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public int getResends() { return resends; }
    public void setResends(int resends) { this.resends = resends; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
