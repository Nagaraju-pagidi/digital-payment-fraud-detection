package com.fraudshield.notification.web;

import com.fraudshield.notification.domain.Notification;
import com.fraudshield.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Used by the Control Center "System" page (role ANALYST via gateway). */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    public record NotificationDto(Long id, Long transactionId, String type, String recipient, String subject,
                                  String status, int attempts, String lastError, Instant createdAt, Instant sentAt) {
    }

    private final NotificationRepository notifications;
    private final CircuitBreakerRegistry breakers;

    public NotificationController(NotificationRepository notifications, CircuitBreakerRegistry breakers) {
        this.notifications = notifications;
        this.breakers = breakers;
    }

    @GetMapping
    public List<NotificationDto> recent(@RequestParam(defaultValue = "30") int limit) {
        return notifications.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)))
                .stream().map(this::toDto).toList();
    }

    /** Email channel health: circuit breaker state + queue counts. */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        CircuitBreaker cb = breakers.circuitBreaker("email");
        CircuitBreaker.Metrics m = cb.getMetrics();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("circuitBreakerState", cb.getState().name());
        out.put("failureRate", m.getFailureRate());
        out.put("bufferedCalls", m.getNumberOfBufferedCalls());
        out.put("sent", notifications.countByStatus("SENT"));
        out.put("waitingForRetry", notifications.countByStatus("FAILED"));
        out.put("dead", notifications.countByStatus("DEAD"));
        return out;
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTransactionId(), n.getType(), n.getRecipient(), n.getSubject(),
                n.getStatus(), n.getAttempts(), n.getLastError(), n.getCreatedAt(), n.getSentAt());
    }
}
