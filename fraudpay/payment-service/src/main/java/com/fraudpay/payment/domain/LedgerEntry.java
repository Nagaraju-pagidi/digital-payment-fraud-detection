package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** One line of the account statement (passbook). */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "entry_type", nullable = false)
    private String entryType;   // OPENING | DEBIT | CREDIT | REFUND
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;
    @Column(nullable = false)
    private String description;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected LedgerEntry() {
    }

    public LedgerEntry(Long userId, Long paymentId, String entryType, BigDecimal amount, BigDecimal balanceAfter,
                       String description) {
        this.userId = userId;
        this.paymentId = paymentId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPaymentId() { return paymentId; }
    public String getEntryType() { return entryType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
