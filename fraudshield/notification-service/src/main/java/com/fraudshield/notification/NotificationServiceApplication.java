package com.fraudshield.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FraudShield notification-service (port 8083).
 * Emails the customer and the fraud team when a payment is held, blocked or released,
 * and when an account is frozen. Protected by circuit breaker + retry + a retry queue.
 * All emails land in Mailpit: http://localhost:8025
 */
@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
