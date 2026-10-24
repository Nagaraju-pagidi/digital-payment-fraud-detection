package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional Outbox row.
 * Saved in the SAME database transaction as the payment, so either both exist or neither does.
 * OutboxPublisher later sends it to FraudShield over internal HTTP and marks it published.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, String aggregateType, String aggregateId, String eventType,
                       String topic, String messageKey, String payload) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getMessageKey() { return messageKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
}
