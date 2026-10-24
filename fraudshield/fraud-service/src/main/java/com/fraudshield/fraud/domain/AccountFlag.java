package com.fraudshield.fraud.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** A customer account that FraudShield has frozen (or unfrozen). */
@Entity
@Table(name = "account_flags")
public class AccountFlag {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name")
    private String userName;
    @Column(name = "user_email")
    private String userEmail;
    @Column(name = "user_upi")
    private String userUpi;

    @Column(nullable = false)
    private boolean frozen;

    private String reason;

    @Column(name = "frozen_at")
    private Instant frozenAt;
    @Column(name = "unfrozen_at")
    private Instant unfrozenAt;
    @Column(name = "unfrozen_by")
    private String unfrozenBy;

    protected AccountFlag() {
    }

    public AccountFlag(Long userId) {
        this.userId = userId;
    }

    public void freeze(String userName, String userEmail, String userUpi, String reason) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userUpi = userUpi;
        this.frozen = true;
        this.reason = reason;
        this.frozenAt = Instant.now();
        this.unfrozenAt = null;
        this.unfrozenBy = null;
    }

    public void unfreeze(String by) {
        this.frozen = false;
        this.unfrozenAt = Instant.now();
        this.unfrozenBy = by;
    }

    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getUserUpi() { return userUpi; }
    public boolean isFrozen() { return frozen; }
    public String getReason() { return reason; }
    public Instant getFrozenAt() { return frozenAt; }
    public Instant getUnfrozenAt() { return unfrozenAt; }
    public String getUnfrozenBy() { return unfrozenBy; }
}
