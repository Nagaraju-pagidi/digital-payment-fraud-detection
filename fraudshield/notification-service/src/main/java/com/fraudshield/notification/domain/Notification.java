package com.fraudshield.notification.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private Long transactionId;
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private String status;   // SENT | FAILED | DEAD

    @Column(nullable = false)
    private int attempts;
    @Column(name = "last_error")
    private String lastError;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
    }

    public Notification(Long transactionId, Long userId, String type, String recipient, String subject, String body) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.type = type;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = "FAILED";
    }

    public void markSent() {
        this.status = "SENT";
        this.attempts++;
        this.sentAt = Instant.now();
        this.lastError = null;
        this.nextAttemptAt = null;
    }

    public void markFailed(String error, Instant nextAttempt, boolean dead) {
        this.attempts++;
        this.status = dead ? "DEAD" : "FAILED";
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        this.nextAttemptAt = dead ? null : nextAttempt;
    }

    public Long getId() { return id; }
    public Long getTransactionId() { return transactionId; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
