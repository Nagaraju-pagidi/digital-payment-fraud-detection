package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.Payment;
import com.fraudpay.payment.domain.PaymentStatus;
import com.fraudpay.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Saga timeout: payments stuck in PROCESSING are moved to ON_HOLD. */
@Component
public class SagaTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutJob.class);

    private final PaymentRepository payments;
    private final DecisionService decisionService;
    private final long timeoutSeconds;

    public SagaTimeoutJob(PaymentRepository payments, DecisionService decisionService,
                          @Value("${fraudpay.saga-timeout-seconds:60}") long timeoutSeconds) {
        this.payments = payments;
        this.decisionService = decisionService;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 15000)
    public void checkStalePayments() {
        Instant threshold = Instant.now().minusSeconds(timeoutSeconds);
        List<Payment> stale = payments.findByStatusAndCreatedAtBefore(PaymentStatus.PROCESSING, threshold);
        if (stale.isEmpty()) return;
        int moved = decisionService.parkStalePayments(stale);
        if (moved > 0) {
            log.warn("Saga timeout: {} payment(s) got no FraudShield answer in {}s -> ON_HOLD", moved, timeoutSeconds);
        }
    }
}
