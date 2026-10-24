package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.OutboxEvent;
import com.fraudpay.payment.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.util.List;

/**
 * Lightweight transactional-outbox dispatcher. Kafka is intentionally not required in the
 * student/demo profile: payment events are delivered to FraudShield over internal HTTP.
 * Rows remain pending and are retried if FraudShield is temporarily unavailable.
 */
@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final RestClient client;
    public OutboxPublisher(OutboxRepository outbox, RestClient.Builder builder,
                           @Value("${fraudshield.internal-url:http://fraud-service:8082}") String baseUrl) {
        this.outbox = outbox;
        this.client = builder.baseUrl(baseUrl).build();
    }
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outbox.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                if (!"PaymentRequested".equals(event.getEventType())) {
                    // The demo profile only dispatches payment requests; other legacy event types
                    // are retained for schema compatibility and should not block payment delivery.
                    event.markPublished();
                    continue;
                }
                client.post().uri("/internal/analyze")
                    .contentType(MediaType.APPLICATION_JSON).body(event.getPayload()).retrieve().toBodilessEntity();
                event.markPublished();
            } catch (Exception ex) {
                log.warn("FraudShield HTTP endpoint unavailable; outbox event {} will retry: {}", event.getId(), ex.getMessage());
                break;
            }
        }
    }
}
