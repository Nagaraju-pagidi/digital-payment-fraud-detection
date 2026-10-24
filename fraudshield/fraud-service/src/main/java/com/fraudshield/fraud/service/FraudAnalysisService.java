package com.fraudshield.fraud.service;

import com.fraudshield.fraud.domain.CaseStatus;
import com.fraudshield.fraud.domain.FraudCase;
import com.fraudshield.fraud.domain.ProcessedEvent;
import com.fraudshield.fraud.dto.CaseDto;
import com.fraudshield.fraud.event.FraudDecisionEvent;
import com.fraudshield.fraud.event.PaymentRequestedEvent;
import com.fraudshield.fraud.event.RuleHit;
import com.fraudshield.fraud.repository.FraudCaseRepository;
import com.fraudshield.fraud.repository.ProcessedEventRepository;
import com.fraudshield.fraud.repository.ProfileRepository;
import com.fraudshield.fraud.rules.FraudContext;
import com.fraudshield.fraud.rules.Geo;
import com.fraudshield.fraud.rules.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The heart of FraudShield.
 * For every payment: build the context (profile, device, receiver, location, velocity),
 * run the rule engine, save the case, publish the verdict, and freeze the account if needed.
 */
@Service
public class FraudAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FraudAnalysisService.class);
    private static final List<CaseStatus> BAD = List.of(CaseStatus.BLOCKED, CaseStatus.ANALYST_REJECTED);

    private final FraudCaseRepository cases;
    private final ProfileRepository profiles;
    private final ProcessedEventRepository processed;
    private final RuleEngine engine;
    private final EventPublisher publisher;
    private final AccountService accounts;
    private final LiveFeedService liveFeed;
    private final CaseMapper mapper;
    private final ZoneId zone;
    private final int freezeScore;
    private final int freezeBlockedCount;
    private final RestClient mlClient;

    public FraudAnalysisService(FraudCaseRepository cases, ProfileRepository profiles,
                                ProcessedEventRepository processed, RuleEngine engine, EventPublisher publisher,
                                AccountService accounts, LiveFeedService liveFeed, CaseMapper mapper,
                                @Value("${fraudshield.timezone:Asia/Kolkata}") String zone,
                                @Value("${fraudshield.thresholds.freeze-score:120}") int freezeScore,
                                @Value("${fraudshield.thresholds.freeze-blocked-count:3}") int freezeBlockedCount,
                                RestClient.Builder restClientBuilder,
                                @Value("${fraudshield.ml-url:http://ml-fraud-service:8091}") String mlUrl) {
        this.cases = cases;
        this.profiles = profiles;
        this.processed = processed;
        this.engine = engine;
        this.publisher = publisher;
        this.accounts = accounts;
        this.liveFeed = liveFeed;
        this.mapper = mapper;
        this.zone = ZoneId.of(zone);
        this.freezeScore = freezeScore;
        this.freezeBlockedCount = freezeBlockedCount;
        this.mlClient = restClientBuilder.baseUrl(mlUrl).build();
    }

    @Transactional
    public void analyze(PaymentRequestedEvent e) {
        // Idempotent consumer: the same event is never scored twice
        if (processed.existsById(e.eventId()) || cases.existsById(e.transactionId())) {
            log.info("Payment #{} already analysed - duplicate event skipped", e.transactionId());
            return;
        }
        processed.save(new ProcessedEvent(e.eventId()));

        Instant at = e.createdAt() == null ? Instant.now() : e.createdAt();
        long senderId = e.senderId();

        // ---- 1. Build the context ----
        var profile = profiles.find(senderId).orElse(null);
        boolean knownDevice = e.deviceId() != null
                && (Objects.equals(e.deviceId(), e.senderRegisteredDeviceId()) || profiles.isKnownDevice(senderId, e.deviceId()));
        boolean knownReceiver = profiles.isKnownReceiver(senderId, e.receiverUpi());
        long recent = cases.countBySenderIdAndCreatedAtAfter(senderId, at.minus(Duration.ofMinutes(10)));
        Double distance = (e.latitude() != null && e.longitude() != null && e.senderHomeLat() != null)
                ? Math.round(Geo.distanceKm(e.senderHomeLat(), e.senderHomeLon(), e.latitude(), e.longitude()) * 10) / 10.0
                : null;
        ZonedDateTime local = at.atZone(zone);
        FraudContext ctx = new FraudContext(e, profile, knownDevice, knownReceiver, recent, distance, local);

        // ---- 2. Evaluate behavioral rules and the trained ML model ----
        RuleEngine.Result ruleResult = engine.evaluate(ctx);
        int mlScore = 0;
        String mlModel = "unavailable";
        try {
            long accountAgeDays = e.senderAccountCreatedAt() == null ? 0L
                    : Math.max(0L, Duration.between(e.senderAccountCreatedAt(), at).toDays());
            double balanceRatio = e.senderBalanceBefore() == null || e.senderBalanceBefore().signum() <= 0 ? 0.0
                    : Math.min(1.0, e.amount().doubleValue() / e.senderBalanceBefore().doubleValue());
            Map<String, Object> mlRequest = new java.util.HashMap<>();
            mlRequest.put("customer_id", String.valueOf(senderId));
            mlRequest.put("occurred_at", at.toString());
            mlRequest.put("amount", e.amount().doubleValue());
            mlRequest.put("amount_to_average", profile == null || profile.average() == null || profile.average().signum() <= 0 ? 1.0 : e.amount().doubleValue() / profile.average().doubleValue());
            mlRequest.put("recent_transactions", recent);
            mlRequest.put("new_device", !knownDevice);
            mlRequest.put("known_receiver", knownReceiver);
            mlRequest.put("impossible_travel", ctx.hasLocation() && distance != null && distance > 500 && profile != null && profile.lastPaymentAt() != null && Duration.between(profile.lastPaymentAt(), at).toMinutes() < 30);
            mlRequest.put("account_age_days", accountAgeDays);
            mlRequest.put("balance_ratio", balanceRatio);
            mlRequest.put("new_account_no_history", accountAgeDays < 7 && (profile == null || profile.approvedCount() == 0));
            mlRequest.put("latitude", e.latitude());
            mlRequest.put("longitude", e.longitude());
            mlRequest.put("city", e.locationCity());
            mlRequest.put("device_id", e.deviceId());
            mlRequest.put("receiver", e.receiverUpi());
            Map<?, ?> prediction = mlClient.post().uri("/score")
                    .contentType(MediaType.APPLICATION_JSON).body(mlRequest).retrieve().body(Map.class);
            if (prediction != null && prediction.get("riskScore") instanceof Number n) mlScore = Math.max(0, Math.min(100, n.intValue()));
            if (prediction != null && prediction.get("model") != null) mlModel = String.valueOf(prediction.get("model"));
        } catch (Exception ex) {
            log.warn("ML scoring service unavailable; using rule baseline for this payment: {}", ex.getMessage());
        }
        // Blend rule evidence with the model. A large amount by itself must not auto-block a payment.
        boolean impossibleTravelHit = ruleResult.hits().stream().anyMatch(h -> "R03".equals(h.ruleId()));
        int combinedScore = mlModel.equals("unavailable") ? ruleResult.score()
                : impossibleTravelHit ? Math.max(ruleResult.score(), mlScore)
                : (int) Math.round(ruleResult.score() * 0.40 + mlScore * 0.60);
        List<RuleHit> combinedHits = new ArrayList<>(ruleResult.hits());
        if (!mlModel.equals("unavailable") && mlScore >= 20) {
            combinedHits.add(new RuleHit("ML01", "ML fraud prediction", mlScore,
                    "Model " + mlModel + " predicted risk score " + mlScore + "/100 based on transaction behavior and context"));
        }
        String combinedDecision = combinedScore >= engine.blockThreshold() ? "BLOCKED"
                : combinedScore >= engine.holdThreshold() ? "HOLD" : "APPROVED";
        RuleEngine.Result result = new RuleEngine.Result(combinedScore, combinedDecision, List.copyOf(combinedHits));

        // ---- 3. Save the case ----
        FraudCase c = new FraudCase();
        c.setTransactionId(e.transactionId());
        c.setChannel(e.channel() == null ? "UPI" : e.channel());
        c.setSenderId(senderId);
        c.setSenderName(e.senderName());
        c.setSenderEmail(e.senderEmail());
        c.setSenderUpi(e.senderUpi());
        c.setReceiverName(e.receiverName());
        c.setReceiverUpi(e.receiverUpi());
        c.setAmount(e.amount());
        c.setNote(e.note());
        c.setDeviceId(e.deviceId());
        c.setDeviceLabel(e.deviceLabel());
        c.setNewDevice(!knownDevice);
        c.setLatitude(e.latitude());
        c.setLongitude(e.longitude());
        c.setLocationCity(e.locationCity());
        c.setLocationCountry(e.locationCountry());
        c.setHomeCity(e.senderHomeCity());
        c.setHomeCountry(e.senderHomeCountry());
        c.setDistanceFromHomeKm(distance);
        c.setScore(result.score());
        c.setDecision(result.decision());
        c.setStatus(CaseStatus.valueOf(result.decision()));
        c.setRuleHits(mapper.hitsJson(result.hits()));
        c.setSimulated(e.simulated());
        c.setCreatedAt(at);
        cases.save(c);

        // ---- 4. Learn from good payments ----
        if ("APPROVED".equals(result.decision())) {
            learn(c);
        }

        // ---- 5. Publish the verdict to FraudPay (and notification / AI services) ----
        publisher.publishDecision(toEvent(c, result.hits(), "AUTO", null));

        log.info("Payment #{} {} -> {} Rs {} | score {} -> {} | {}", c.getTransactionId(), c.getSenderUpi(),
                c.getReceiverUpi(), c.getAmount(), result.score(), result.decision(),
                result.hits().stream().map(RuleHit::ruleId).toList());

        // ---- 6. Freeze the account on very high risk or repeated fraud ----
        if ("BLOCKED".equals(result.decision())) {
            long blocked24h = cases.countBySenderIdAndStatusInAndCreatedAtAfter(senderId, BAD, at.minus(Duration.ofHours(24)));
            String reason = null;
            if (result.score() >= freezeScore) {
                reason = "Critical risk score " + result.score() + " on payment #" + c.getTransactionId();
            } else if (blocked24h >= freezeBlockedCount) {
                reason = blocked24h + " blocked payments in 24 hours";
            }
            if (reason != null && accounts.freeze(senderId, e.senderName(), e.senderEmail(), e.senderUpi(), reason)) {
                c.setAccountFrozen(true);
            }
        }

        CaseDto dto = mapper.toDto(c);
        AfterCommit.run(() -> liveFeed.broadcast("case", dto));
    }

    /** Add an approved payment to the customer's normal-behaviour profile. */
    public void learn(FraudCase c) {
        boolean night = c.getCreatedAt().atZone(zone).getHour() < 5;
        profiles.recordApproved(c.getSenderId(), c.getAmount(), night, c.getCreatedAt(),
                c.getLatitude(), c.getLongitude(), c.getLocationCity(), c.getDeviceId(), c.getReceiverUpi());
        // Update the ML service's per-customer baseline only after the engine's final APPROVED decision.
        try {
            Map<String, Object> feedback = new java.util.HashMap<>();
            feedback.put("customer_id", String.valueOf(c.getSenderId()));
            feedback.put("event_type", "PAYMENT");
            feedback.put("occurred_at", c.getCreatedAt().toString());
            feedback.put("amount", c.getAmount());
            feedback.put("latitude", c.getLatitude());
            feedback.put("longitude", c.getLongitude());
            feedback.put("city", c.getLocationCity());
            feedback.put("device_id", c.getDeviceId());
            feedback.put("receiver", c.getReceiverUpi());
            feedback.put("outcome", "APPROVED");
            mlClient.post().uri("/feedback").contentType(MediaType.APPLICATION_JSON).body(feedback).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Could not update ML behaviour history for customer {}: {}", c.getSenderId(), ex.getMessage());
        }
    }

    public FraudDecisionEvent toEvent(FraudCase c, List<RuleHit> hits, String source, String analystNote) {
        String decision = switch (c.getStatus()) {
            case APPROVED, ANALYST_APPROVED -> "APPROVED";
            case HOLD -> "HOLD";
            case BLOCKED, ANALYST_REJECTED -> "BLOCKED";
        };
        List<String> reasons = hits.stream().map(RuleHit::detail).toList();
        return new FraudDecisionEvent(UUID.randomUUID().toString(), c.getTransactionId(), c.getSenderId(),
                c.getSenderName(), c.getSenderEmail(), c.getSenderUpi(), c.getReceiverUpi(), c.getReceiverName(),
                c.getAmount(), c.getScore(), decision, reasons, hits, source, analystNote, c.isSimulated(), Instant.now());
    }
}
