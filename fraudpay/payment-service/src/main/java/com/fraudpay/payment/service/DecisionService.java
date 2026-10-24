package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.LedgerEntry;
import com.fraudpay.payment.domain.Payment;
import com.fraudpay.payment.domain.PaymentStatus;
import com.fraudpay.payment.domain.ProcessedEvent;
import com.fraudpay.payment.domain.User;
import com.fraudpay.payment.domain.UserStatus;
import com.fraudpay.payment.event.AccountCommandEvent;
import com.fraudpay.payment.event.FraudDecisionEvent;
import com.fraudpay.payment.repository.LedgerRepository;
import com.fraudpay.payment.repository.PaymentRepository;
import com.fraudpay.payment.security.KycValidator;
import com.fraudpay.payment.repository.ProcessedEventRepository;
import com.fraudpay.payment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Step 2 of the payment saga: react to FraudShield's verdict.
 *   APPROVED -> move the held money to the receiver        (SUCCESS)
 *   HOLD     -> keep holding, wait for an analyst           (ON_HOLD)
 *   BLOCKED  -> give the money back to the sender           (BLOCKED / REJECTED)
 *              ^ this refund is the saga's "compensating action"
 * Every event is handled at most once (idempotent consumer).
 */
@Service
public class DecisionService {

    private static final Logger log = LoggerFactory.getLogger(DecisionService.class);

    private final PaymentRepository payments;
    private final UserRepository users;
    private final ProcessedEventRepository processed;
    private final LiveUpdateService live;
    private final LedgerRepository ledger;

    public DecisionService(PaymentRepository payments, UserRepository users,
                           ProcessedEventRepository processed, LiveUpdateService live, LedgerRepository ledger) {
        this.ledger = ledger;
        this.payments = payments;
        this.users = users;
        this.processed = processed;
        this.live = live;
    }

    @Transactional
    public void handleDecision(FraudDecisionEvent e) {
        if (processed.existsById(e.eventId())) {
            log.info("Duplicate decision event {} skipped", e.eventId());
            return;
        }
        processed.save(new ProcessedEvent(e.eventId()));

        Optional<Payment> found = payments.findByIdForUpdate(e.transactionId());
        if (found.isEmpty()) {
            log.warn("Decision for unknown payment #{}", e.transactionId());
            return;
        }
        Payment p = found.get();
        boolean byAnalyst = "ANALYST".equals(e.source());
        p.setFraudScore(e.score());
        p.setFraudReasons(e.reasons() == null ? null : String.join(" | ", e.reasons()));

        User receiverToNotify = null;
        User senderAfter = null;

        switch (e.decision()) {
            case "APPROVED" -> {
                if (p.getStatus().isOpen()) {
                    User receiver = users.findByIdForUpdate(p.getReceiverId()).orElseThrow();
                    receiver.credit(p.getAmount());
                    ledger.save(new LedgerEntry(receiver.getId(), p.getId(), "CREDIT", p.getAmount(), receiver.getBalance(),
                            p.getChannel() + " from " + p.getSenderName() + (p.getNote() == null ? "" : " - " + p.getNote())));
                    p.changeStatus(PaymentStatus.SUCCESS, byAnalyst
                            ? "Approved after review by FraudShield analyst"
                            : "Paid successfully");
                    receiverToNotify = receiver;
                }
            }
            case "HOLD" -> {
                if (p.getStatus() == PaymentStatus.PROCESSING) {
                    p.changeStatus(PaymentStatus.ON_HOLD,
                            "On hold - FraudShield is verifying this payment for your safety");
                }
            }
            case "BLOCKED" -> {
                if (p.getStatus().isOpen()) {
                    User sender = users.findByIdForUpdate(p.getSenderId()).orElseThrow();
                    sender.credit(p.getAmount()); // compensating action: refund
                    ledger.save(new LedgerEntry(sender.getId(), p.getId(), "REFUND", p.getAmount(), sender.getBalance(),
                            "Reversal of " + p.getChannel() + " to " + p.getReceiverName() + " (A/c "
                                    + KycValidator.maskAccount(p.getReceiverAccount()) + ") - stopped by fraud check"));
                    p.changeStatus(byAnalyst ? PaymentStatus.REJECTED : PaymentStatus.BLOCKED,
                            (byAnalyst ? "Rejected by FraudShield analyst" : "Blocked by FraudShield")
                                    + " - Rs " + p.getAmount().toPlainString() + " refunded to your account");
                    senderAfter = sender;
                }
            }
            default -> log.warn("Unknown decision {} for payment #{}", e.decision(), p.getId());
        }

        log.info("Payment #{} -> {} (score {}, by {})", p.getId(), p.getStatus(), e.score(), e.source());

        // Push live updates to the phones only after the DB commit succeeded
        final Long senderId = p.getSenderId();
        final var senderView = Mapper.toDto(p, senderId);
        final var senderAccount = senderAfter == null ? null : Mapper.toDto(senderAfter);
        final User receiver = receiverToNotify;
        final var receiverView = receiver == null ? null : Mapper.toDto(p, receiver.getId());
        final var receiverAccount = receiver == null ? null : Mapper.toDto(receiver);
        AfterCommit.run(() -> {
            live.send(senderId, "payment", senderView);
            if (senderAccount != null) live.send(senderId, "account", senderAccount);
            if (receiver != null) {
                live.send(receiver.getId(), "payment", receiverView);
                live.send(receiver.getId(), "account", receiverAccount);
            }
        });
    }

    @Transactional
    public void handleAccountCommand(AccountCommandEvent e) {
        if (processed.existsById(e.eventId())) return;
        processed.save(new ProcessedEvent(e.eventId()));

        users.findByIdForUpdate(e.userId()).ifPresent(user -> {
            if ("FREEZE".equals(e.action())) {
                user.setStatus(UserStatus.FROZEN);
                user.setStatusReason(e.reason());
            } else if ("UNFREEZE".equals(e.action())) {
                user.setStatus(UserStatus.ACTIVE);
                user.setStatusReason(null);
            }
            log.warn("Account {} is now {} ({})", user.getUpiId(), user.getStatus(), e.reason());
            final var dto = Mapper.toDto(user);
            AfterCommit.run(() -> live.send(user.getId(), "account", dto));
        });
    }

    /**
     * Saga timeout: if FraudShield has not answered within the time limit
     * (for example the fraud-service is down), the payment is parked ON_HOLD.
     * The money stays safely held; when FraudShield comes back it still decides.
     */
    @Transactional
    public int parkStalePayments(List<Payment> stale) {
        int count = 0;
        for (Payment candidate : stale) {
            Payment p = payments.findByIdForUpdate(candidate.getId()).orElse(null);
            if (p == null || p.getStatus() != PaymentStatus.PROCESSING) continue;
            p.changeStatus(PaymentStatus.ON_HOLD,
                    "FraudShield is taking longer than usual - your money is held safely");
            count++;
            final Long senderId = p.getSenderId();
            final var dto = Mapper.toDto(p, senderId);
            AfterCommit.run(() -> live.send(senderId, "payment", dto));
        }
        return count;
    }
}
