package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** A payee added by the customer for net-banking transfers. */
@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "payee_user_id", nullable = false)
    private Long payeeUserId;
    @Column(nullable = false)
    private String name;
    private String nickname;
    @Column(name = "account_number", nullable = false)
    private String accountNumber;
    @Column(nullable = false)
    private String ifsc;
    /** Cooling period: transfers are allowed only after this time. */
    @Column(name = "active_from", nullable = false)
    private Instant activeFrom;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private boolean deleted;

    protected Beneficiary() {
    }

    public Beneficiary(Long userId, Long payeeUserId, String name, String nickname, String accountNumber,
                       String ifsc, Instant activeFrom) {
        this.userId = userId;
        this.payeeUserId = payeeUserId;
        this.name = name;
        this.nickname = nickname;
        this.accountNumber = accountNumber;
        this.ifsc = ifsc;
        this.activeFrom = activeFrom;
    }

    public boolean isActive() {
        return !deleted && !activeFrom.isAfter(Instant.now());
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPayeeUserId() { return payeeUserId; }
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getAccountNumber() { return accountNumber; }
    public String getIfsc() { return ifsc; }
    public Instant getActiveFrom() { return activeFrom; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
