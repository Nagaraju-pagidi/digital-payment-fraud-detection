package com.fraudpay.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudpay.payment.domain.*;
import com.fraudpay.payment.dto.*;
import com.fraudpay.payment.event.PaymentRequestedEvent;
import com.fraudpay.payment.event.Topics;
import com.fraudpay.payment.geo.GeoCatalog;
import com.fraudpay.payment.repository.*;
import com.fraudpay.payment.security.KycValidator;
import com.fraudpay.payment.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Step 1 of the payment saga - for net banking (IMPS/NEFT) and UPI.
 * In ONE database transaction:
 *   - check transaction PIN (+ email OTP for net banking), limits, balance, account status
 *   - take the money from the sender and HOLD it (status PROCESSING) + statement entry
 *   - write a PaymentRequested event into the outbox table
 * The money reaches the receiver only after FraudShield approves (see DecisionService).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record PendingTransfer(Long payeeId, BigDecimal amount, String mode, String remarks, String deviceId,
                                  String deviceLabel, Double latitude, Double longitude) {
    }

    private final UserRepository users;
    private final PaymentRepository payments;
    private final OutboxRepository outbox;
    private final BeneficiaryRepository payees;
    private final LedgerRepository ledger;
    private final AccountService accounts;
    private final OtpService otp;
    private final GeoCatalog geo;
    private final ObjectMapper objectMapper;
    private final LiveUpdateService live;
    private final BigDecimal impsLimit;
    private final BigDecimal neftLimit;
    private final BigDecimal netbankingDaily;
    private final BigDecimal upiLimit;
    private final BigDecimal upiDaily;

    public PaymentService(UserRepository users, PaymentRepository payments, OutboxRepository outbox,
                          BeneficiaryRepository payees, LedgerRepository ledger, AccountService accounts,
                          OtpService otp, GeoCatalog geo, ObjectMapper objectMapper, LiveUpdateService live,
                          @Value("${fraudpay.limits.imps-per-transaction:500000}") BigDecimal impsLimit,
                          @Value("${fraudpay.limits.neft-per-transaction:1000000}") BigDecimal neftLimit,
                          @Value("${fraudpay.limits.netbanking-per-day:1000000}") BigDecimal netbankingDaily,
                          @Value("${fraudpay.limits.upi-per-transaction:100000}") BigDecimal upiLimit,
                          @Value("${fraudpay.limits.upi-per-day:100000}") BigDecimal upiDaily) {
        this.users = users;
        this.payments = payments;
        this.outbox = outbox;
        this.payees = payees;
        this.ledger = ledger;
        this.accounts = accounts;
        this.otp = otp;
        this.geo = geo;
        this.objectMapper = objectMapper;
        this.live = live;
        this.impsLimit = impsLimit;
        this.neftLimit = neftLimit;
        this.netbankingDaily = netbankingDaily;
        this.upiLimit = upiLimit;
        this.upiDaily = upiDaily;
    }

    // =====================================================================
    //  Net banking transfer - step 1: payee, limits, transaction PIN  ->  email OTP
    // =====================================================================
    @Transactional(noRollbackFor = ApiException.class)   // wrong-PIN count must be saved
    public OtpChallengeResponse startTransfer(Long userId, TransferRequest r) {
        User sender = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
        String mode = r.mode().trim().toUpperCase(Locale.ROOT);
        if (!mode.equals("IMPS") && !mode.equals("NEFT")) throw ApiException.badRequest("Choose IMPS or NEFT");
        Beneficiary payee = payees.findByIdAndUserIdAndDeletedFalse(r.payeeId(), userId)
                .orElseThrow(() -> ApiException.notFound("Payee not found"));
        if (!payee.isActive()) {
            throw ApiException.badRequest("This payee was added recently. For your safety, transfers are allowed after "
                    + java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                    .withZone(java.time.ZoneId.of("Asia/Kolkata")).format(payee.getActiveFrom()) + ".");
        }
        BigDecimal amount = r.amount().setScale(2, RoundingMode.HALF_UP);
        checkBasics(sender, amount);
        BigDecimal perTxn = mode.equals("IMPS") ? impsLimit : neftLimit;
        if (amount.compareTo(perTxn) > 0) {
            throw ApiException.badRequest(mode + " limit is Rs " + perTxn.toPlainString() + " per transaction");
        }
        BigDecimal today = accounts.sentToday(userId, List.of("IMPS", "NEFT"));
        if (today.add(amount).compareTo(netbankingDaily) > 0) {
            throw ApiException.badRequest("Daily net-banking limit of Rs " + netbankingDaily.toPlainString()
                    + " exceeded (already sent Rs " + today.toPlainString() + " today)");
        }
        accounts.checkPin(sender, r.pin());

        return otp.start(userId, OtpService.TRANSFER, sender.getEmail(),
                mode + " transfer of Rs " + amount.toPlainString() + " to " + payee.getName()
                        + " (A/c " + KycValidator.maskAccount(payee.getAccountNumber()) + ")",
                new PendingTransfer(payee.getId(), amount, mode, r.remarks(), r.deviceId(), r.deviceLabel(),
                        r.latitude(), r.longitude()));
    }

    // =====================================================================
    //  Net banking transfer - step 2: correct OTP -> money held, sent to FraudShield
    // =====================================================================
    @Transactional
    public PaymentDto confirmTransfer(Long userId, OtpVerifyRequest r) {
        OtpChallenge c = otp.verify(r.challengeId(), OtpService.TRANSFER, userId, r.otp());
        PendingTransfer t = otp.payload(c, PendingTransfer.class);
        User sender = users.findByIdForUpdate(userId).orElseThrow();
        Beneficiary payee = payees.findByIdAndUserIdAndDeletedFalse(t.payeeId(), userId)
                .orElseThrow(() -> ApiException.notFound("Payee not found"));
        User receiver = users.findById(payee.getPayeeUserId())
                .orElseThrow(() -> ApiException.notFound("Payee account not found"));
        Payment p = createPayment(sender, receiver, t.amount(), t.remarks(), t.mode(), payee.getCreatedAt(),
                t.deviceId(), t.deviceLabel(), t.latitude(), t.longitude(), false);
        return Mapper.toDto(p, userId);
    }

    // =====================================================================
    //  UPI payment: UPI ID + amount + transaction PIN
    // =====================================================================
    @Transactional(noRollbackFor = ApiException.class)
    public PaymentDto payUpi(Long userId, UpiPaymentRequest r) {
        User sender = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
        User receiver = users.findByUpiIdIgnoreCase(r.upiId().trim())
                .orElseThrow(() -> ApiException.notFound("No account linked to UPI ID " + r.upiId()));
        BigDecimal amount = r.amount().setScale(2, RoundingMode.HALF_UP);
        checkBasics(sender, amount);
        if (receiver.getId().equals(sender.getId())) throw ApiException.badRequest("You cannot pay yourself");
        if (amount.compareTo(upiLimit) > 0) {
            throw ApiException.badRequest("UPI limit is Rs " + upiLimit.toPlainString() + " per transaction");
        }
        BigDecimal today = accounts.sentToday(userId, List.of("UPI"));
        if (today.add(amount).compareTo(upiDaily) > 0) {
            throw ApiException.badRequest("Daily UPI limit of Rs " + upiDaily.toPlainString() + " exceeded");
        }
        accounts.checkPin(sender, r.pin());
        Payment p = createPayment(sender, receiver, amount, r.note(), "UPI", null, r.deviceId(), r.deviceLabel(),
                r.latitude(), r.longitude(), false);
        return Mapper.toDto(p, userId);
    }

    @Transactional(readOnly = true)
    public Map<String, String> lookupUpi(String upiId) {
        User u = users.findByUpiIdIgnoreCase(upiId.trim())
                .orElseThrow(() -> ApiException.notFound("No account linked to UPI ID " + upiId));
        return Map.of("fullName", u.getFullName(), "upiId", u.getUpiId());
    }

    private void checkBasics(User sender, BigDecimal amount) {
        if (sender.isFrozen()) {
            throw new ApiException(HttpStatus.LOCKED,
                    "Your account is frozen by our fraud team for your safety. Please contact the bank.");
        }
        if (amount.signum() <= 0) throw ApiException.badRequest("Amount must be more than zero");
        if (sender.getBalance().compareTo(amount) < 0) throw ApiException.badRequest("Insufficient balance");
    }

    /**
     * Shared by all channels and by the traffic simulator.
     * The caller must hold the sender's row lock (findByIdForUpdate). All checks happen before any change.
     */
    @Transactional
    public Payment createPayment(User sender, User receiver, BigDecimal rawAmount, String note, String channel,
                                 Instant payeeAddedAt, String deviceId, String deviceLabel, Double lat, Double lon,
                                 boolean simulated) {
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.HALF_UP);
        checkBasics(sender, amount);
        if (receiver.getId().equals(sender.getId())) throw ApiException.badRequest("You cannot pay yourself");

        // Where is the customer right now? (browser GPS)
        GeoCatalog.GeoMatch where = (lat != null && lon != null) ? geo.nearest(lat, lon) : null;

        BigDecimal balanceBefore = sender.getBalance();
        // HOLD the money: it leaves the sender now, reaches the receiver only after approval
        sender.debit(amount);

        Payment p = new Payment();
        p.setReferenceNo(referenceNo(channel));
        p.setChannel(channel);
        p.setSenderId(sender.getId());
        p.setSenderName(sender.getFullName());
        p.setSenderUpi(sender.getUpiId());
        p.setReceiverId(receiver.getId());
        p.setReceiverName(receiver.getFullName());
        p.setReceiverUpi(receiver.getUpiId());
        p.setReceiverAccount(receiver.getAccountNumber());
        p.setPayeeAddedAt(payeeAddedAt);
        p.setAmount(amount);
        p.setNote(note == null || note.isBlank() ? null : note.trim());
        p.setStatusMessage("Security check in progress");
        p.setDeviceId(deviceId);
        p.setDeviceLabel(deviceLabel);
        p.setLatitude(lat);
        p.setLongitude(lon);
        p.setLocationCity(where == null ? null : where.city());
        p.setLocationCountry(where == null ? null : where.country());
        p.setSimulated(simulated);
        payments.save(p);

        ledger.save(new LedgerEntry(sender.getId(), p.getId(), "DEBIT", amount, sender.getBalance(),
                channel + " to " + receiver.getFullName() + " (A/c " + KycValidator.maskAccount(receiver.getAccountNumber())
                        + ")" + (p.getNote() == null ? "" : " - " + p.getNote())));

        PaymentRequestedEvent event = new PaymentRequestedEvent(
                UUID.randomUUID().toString(), p.getId(),
                sender.getId(), sender.getFullName(), sender.getEmail(), sender.getUpiId(),
                sender.getHomeCity(), sender.getHomeCountry(), sender.getHomeLat(), sender.getHomeLon(),
                sender.getCreatedAt(), sender.getRegisteredDeviceId(), balanceBefore,
                receiver.getId(), receiver.getFullName(), receiver.getUpiId(),
                amount, p.getNote(), deviceId, deviceLabel, lat, lon,
                p.getLocationCity(), p.getLocationCountry(), simulated, Instant.now(),
                channel, payeeAddedAt);

        // Transactional Outbox: same DB transaction as the payment
        outbox.save(new OutboxEvent(UUID.randomUUID(), "Payment", String.valueOf(p.getId()),
                "PaymentRequested", Topics.PAYMENT_REQUESTED, String.valueOf(sender.getId()), toJson(event)));

        log.info("Payment #{} {} {} -> {} Rs {} from {} ({}) is PROCESSING", p.getId(), channel, sender.getUsername(),
                receiver.getUsername(), amount, p.getLocationCity(), deviceLabel);

        final Long senderId = sender.getId();
        final PaymentDto dto = Mapper.toDto(p, senderId);
        final UserDto account = Mapper.toDto(sender);
        AfterCommit.run(() -> {
            live.send(senderId, "payment", dto);
            live.send(senderId, "account", account);
        });
        return p;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> history(Long userId) {
        return payments.findHistory(userId, PageRequest.of(0, 200)).stream()
                // receivers only see money that actually arrived
                .filter(p -> p.getSenderId().equals(userId) || p.getStatus() == PaymentStatus.SUCCESS)
                .limit(100)
                .map(p -> Mapper.toDto(p, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentDto get(Long userId, Long paymentId) {
        Payment p = payments.findById(paymentId)
                .filter(x -> x.getSenderId().equals(userId)
                        || (x.getReceiverId().equals(userId) && x.getStatus() == PaymentStatus.SUCCESS))
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        return Mapper.toDto(p, userId);
    }

    private static String referenceNo(String channel) {
        return channel + String.format("%012d", Math.floorMod(RANDOM.nextLong(), 1_000_000_000_000L));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event", e);
        }
    }
}
