package com.fraudshield.notification.service;

import com.fraudshield.notification.domain.Notification;
import com.fraudshield.notification.domain.ProcessedEvent;
import com.fraudshield.notification.event.AccountCommandEvent;
import com.fraudshield.notification.event.FraudDecisionEvent;
import com.fraudshield.notification.repository.NotificationRepository;
import com.fraudshield.notification.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Decides WHO gets WHICH email for each FraudShield event. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final ProcessedEventRepository processed;
    private final EmailSender emailSender;
    private final String fraudTeam;

    public NotificationService(NotificationRepository notifications, ProcessedEventRepository processed,
                               EmailSender emailSender, @Value("${notification.fraud-team}") String fraudTeam) {
        this.notifications = notifications;
        this.processed = processed;
        this.emailSender = emailSender;
        this.fraudTeam = fraudTeam;
    }

    @Transactional
    public void onDecision(FraudDecisionEvent e) {
        if (processed.existsById(e.eventId())) return;
        processed.save(new ProcessedEvent(e.eventId()));
        if (e.simulated()) return; // no emails for simulator bots

        boolean byAnalyst = "ANALYST".equals(e.source());
        String amount = "Rs " + e.amount().toPlainString();
        String reasons = e.reasons() == null || e.reasons().isEmpty() ? "-" : "- " + String.join("\n- ", e.reasons());

        switch (e.decision()) {
            case "HOLD" -> {
                email(e.transactionId(), e.senderId(), "PAYMENT_HOLD", e.senderEmail(),
                        "FraudPay: your payment of " + amount + " is on hold",
                        """
                        Hi %s,

                        Your payment of %s to %s (%s) is ON HOLD for a quick security check.
                        Your money is safe and has not left FraudPay.

                        Why we are checking:
                        %s

                        If this was NOT you, please contact FraudPay support immediately.

                        - FraudShield Security Team
                        """.formatted(e.senderName(), amount, e.receiverName(), e.receiverUpi(), reasons));
                email(e.transactionId(), e.senderId(), "TEAM_REVIEW", fraudTeam,
                        "[REVIEW] Payment #" + e.transactionId() + " held - score " + e.score(),
                        teamAlert(e, reasons, "needs analyst review in the Control Center"));
            }
            case "BLOCKED" -> {
                email(e.transactionId(), e.senderId(), byAnalyst ? "PAYMENT_REJECTED" : "PAYMENT_BLOCKED", e.senderEmail(),
                        "FraudPay: payment of " + amount + " was " + (byAnalyst ? "rejected" : "blocked"),
                        """
                        Hi %s,

                        We %s your payment of %s to %s because it looked like fraud.
                        The full amount has been refunded to your FraudPay wallet.

                        Reasons:
                        %s
                        %s
                        If this WAS you, please contact support to verify your identity.

                        - FraudShield Security Team
                        """.formatted(e.senderName(), byAnalyst ? "rejected" : "blocked", amount, e.receiverUpi(), reasons,
                                e.analystNote() == null ? "" : "\nAnalyst note: " + e.analystNote() + "\n"));
                if (!byAnalyst) {
                    email(e.transactionId(), e.senderId(), "TEAM_BLOCKED", fraudTeam,
                            "[BLOCKED] Payment #" + e.transactionId() + " - score " + e.score(),
                            teamAlert(e, reasons, "was blocked automatically and refunded"));
                }
            }
            case "APPROVED" -> {
                if (byAnalyst) {
                    email(e.transactionId(), e.senderId(), "PAYMENT_RELEASED", e.senderEmail(),
                            "FraudPay: your payment of " + amount + " is complete",
                            """
                            Hi %s,

                            Good news - our security team verified your payment of %s to %s and released it.
                            The money has been delivered.

                            - FraudShield Security Team
                            """.formatted(e.senderName(), amount, e.receiverUpi()));
                }
            }
            default -> log.debug("No email for decision {}", e.decision());
        }
    }

    @Transactional
    public void onAccountCommand(AccountCommandEvent e) {
        if (processed.existsById(e.eventId())) return;
        processed.save(new ProcessedEvent(e.eventId()));
        if (e.userEmail() == null || e.userEmail().contains(".bot@")) return;

        if ("FREEZE".equals(e.action())) {
            email(null, e.userId(), "ACCOUNT_FROZEN", e.userEmail(), "FraudPay: your account has been frozen",
                    """
                    Hi %s,

                    For your safety we have temporarily FROZEN your FraudPay account.
                    Reason: %s

                    No money can be sent until our team verifies your identity.
                    Please contact FraudPay support.

                    - FraudShield Security Team
                    """.formatted(e.userName(), e.reason()));
            email(null, e.userId(), "TEAM_FROZEN", fraudTeam, "[FROZEN] Account " + e.userName(),
                    "Account of " + e.userName() + " (user id " + e.userId() + ") was frozen.\nReason: " + e.reason());
        } else if ("UNFREEZE".equals(e.action())) {
            email(null, e.userId(), "ACCOUNT_RESTORED", e.userEmail(), "FraudPay: your account is active again",
                    """
                    Hi %s,

                    Your FraudPay account has been verified and is ACTIVE again. Thank you for your patience.

                    - FraudShield Security Team
                    """.formatted(e.userName()));
        }
    }

    private String teamAlert(FraudDecisionEvent e, String reasons, String what) {
        return """
                Payment #%d %s.

                Customer : %s (%s)
                Receiver : %s
                Amount   : Rs %s
                Score    : %d

                Rules triggered:
                %s
                """.formatted(e.transactionId(), what, e.senderName(), e.senderUpi(), e.receiverUpi(),
                e.amount().toPlainString(), e.score(), reasons);
    }

    private void email(Long transactionId, Long userId, String type, String to, String subject, String body) {
        if (to == null || to.isBlank()) return;
        Notification n = new Notification(transactionId, userId, type, to, subject, body);
        EmailSender.SendResult result = emailSender.send(to, subject, body);
        if (result.delivered()) {
            n.markSent();
        } else {
            n.markFailed(result.error(), Instant.now().plusSeconds(30), false); // goes to the retry queue
        }
        notifications.save(n);
    }
}
