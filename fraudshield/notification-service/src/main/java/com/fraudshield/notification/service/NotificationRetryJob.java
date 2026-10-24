package com.fraudshield.notification.service;

import com.fraudshield.notification.domain.Notification;
import com.fraudshield.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Database-backed retry queue (dead-letter handling).
 * Every 20s: re-send FAILED emails that are due. Wait time doubles each time
 * (30s, 60s, 2m, 4m ... max 30m). After 8 attempts the email is marked DEAD.
 */
@Component
public class NotificationRetryJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);
    private static final int MAX_ATTEMPTS = 8;
    private static final long BASE_DELAY_SECONDS = 30;
    private static final long MAX_DELAY_SECONDS = 1800;

    private final NotificationRepository notifications;
    private final EmailSender emailSender;

    public NotificationRetryJob(NotificationRepository notifications, EmailSender emailSender) {
        this.notifications = notifications;
        this.emailSender = emailSender;
    }

    @Scheduled(fixedDelay = 20000, initialDelay = 20000)
    @Transactional
    public void retryFailed() {
        List<Notification> due = notifications.findByStatusAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
                "FAILED", Instant.now(), PageRequest.of(0, 20));
        if (due.isEmpty()) return;
        log.info("Retrying {} undelivered email(s)", due.size());
        for (Notification n : due) {
            EmailSender.SendResult result = emailSender.send(n.getRecipient(), n.getSubject(), n.getBody());
            if (result.delivered()) {
                n.markSent();
                log.info("Retry delivered email #{} after {} attempt(s)", n.getId(), n.getAttempts());
            } else {
                boolean dead = n.getAttempts() + 1 >= MAX_ATTEMPTS;
                long delay = Math.min(BASE_DELAY_SECONDS << Math.min(n.getAttempts(), 10), MAX_DELAY_SECONDS);
                n.markFailed(result.error(), Instant.now().plusSeconds(delay), dead);
                if (dead) log.error("Email #{} is DEAD after {} attempts", n.getId(), n.getAttempts());
            }
        }
    }
}
