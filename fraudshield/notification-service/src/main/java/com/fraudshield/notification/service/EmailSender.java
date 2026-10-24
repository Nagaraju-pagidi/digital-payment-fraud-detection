package com.fraudshield.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends one email, protected by Resilience4j:
 *  - @Retry: a failing send is tried 3 times (1s, 2s backoff)
 *  - @CircuitBreaker: if most recent sends fail, the breaker OPENS and we stop hammering
 *    the mail server for 30 seconds (calls fail fast instead of waiting for timeouts)
 *  - fallback: never throws - returns a failed result so the email goes to the retry queue
 * Demo: "docker compose stop mailpit" -> breaker opens; "docker compose start mailpit" -> queue drains.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    public record SendResult(boolean delivered, String error) {
    }

    private final JavaMailSender mailSender;
    private final String from;

    public EmailSender(JavaMailSender mailSender, @Value("${notification.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Retry(name = "email", fallbackMethod = "sendFallback")
    @CircuitBreaker(name = "email")
    public SendResult send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email sent to {}: {}", to, subject);
        return new SendResult(true, null);
    }

    public SendResult sendFallback(String to, String subject, String body, Throwable error) {
        log.warn("Email to {} not delivered ({}): {}", to, error.getClass().getSimpleName(), error.getMessage());
        return new SendResult(false, error.getClass().getSimpleName() + ": " + error.getMessage());
    }
}
