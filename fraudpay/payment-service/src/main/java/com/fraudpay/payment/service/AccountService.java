package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.LedgerEntry;
import com.fraudpay.payment.domain.Payment;
import com.fraudpay.payment.domain.TrustedDevice;
import com.fraudpay.payment.domain.User;
import com.fraudpay.payment.dto.*;
import com.fraudpay.payment.repository.LedgerRepository;
import com.fraudpay.payment.repository.LoginHistoryRepository;
import com.fraudpay.payment.repository.PaymentRepository;
import com.fraudpay.payment.repository.TrustedDeviceRepository;
import com.fraudpay.payment.repository.UserRepository;
import com.fraudpay.payment.security.KycValidator;
import com.fraudpay.payment.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Account summary, statement, profile and security settings of the logged-in customer. */
@Service
public class AccountService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final UserRepository users;
    private final LedgerRepository ledger;
    private final PaymentRepository payments;
    private final TrustedDeviceRepository devices;
    private final LoginHistoryRepository history;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final int maxPinFailures;
    private final long pinLockMinutes;

    public AccountService(UserRepository users, LedgerRepository ledger, PaymentRepository payments,
                          TrustedDeviceRepository devices, LoginHistoryRepository history, PasswordEncoder encoder,
                          MailService mail,
                          @Value("${fraudpay.security.max-pin-failures:3}") int maxPinFailures,
                          @Value("${fraudpay.security.pin-lock-minutes:30}") long pinLockMinutes) {
        this.users = users;
        this.ledger = ledger;
        this.payments = payments;
        this.devices = devices;
        this.history = history;
        this.encoder = encoder;
        this.mail = mail;
        this.maxPinFailures = maxPinFailures;
        this.pinLockMinutes = pinLockMinutes;
    }

    public User require(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return Mapper.toDto(require(userId));
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(Long userId) {
        return Mapper.toProfile(require(userId));
    }

    /**
     * Checks the 6-digit transaction PIN. 3 wrong PINs lock transactions for 30 minutes.
     * Callers must use @Transactional(noRollbackFor = ApiException.class) so the failure count is saved.
     */
    public void checkPin(User u, String pin) {
        if (u.isPinLocked()) {
            throw new ApiException(HttpStatus.LOCKED, "Transactions are locked after " + maxPinFailures
                    + " wrong PINs. Try again after "
                    + DateTimeFormatter.ofPattern("hh:mm a").withZone(IST).format(u.getPinLockedUntil()) + ".");
        }
        if (pin == null || !encoder.matches(pin, u.getPinHash())) {
            u.setPinFailures(u.getPinFailures() + 1);
            int left = maxPinFailures - u.getPinFailures();
            if (left <= 0) {
                u.setPinFailures(0);
                u.setPinLockedUntil(Instant.now().plus(Duration.ofMinutes(pinLockMinutes)));
                mail.notify(u.getEmail(), "Transactions locked",
                        "Your transaction PIN was entered wrongly " + maxPinFailures + " times. Payments are locked for "
                                + pinLockMinutes + " minutes. If this was not you, change your password immediately.");
                throw new ApiException(HttpStatus.LOCKED, "Wrong transaction PIN entered " + maxPinFailures
                        + " times. Transactions are locked for " + pinLockMinutes + " minutes.");
            }
            throw ApiException.badRequest("Incorrect transaction PIN. " + left + " attempt(s) left.");
        }
        u.setPinFailures(0);
    }

    // ---------------- statement ----------------

    @Transactional(readOnly = true)
    public List<StatementEntryDto> statement(Long userId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(IST) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (start.isAfter(end)) throw ApiException.badRequest("'From' date must be before 'To' date");
        if (start.isBefore(end.minusDays(366))) throw ApiException.badRequest("Statement period can be at most 1 year");
        List<LedgerEntry> rows = ledger.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDescIdDesc(userId,
                start.atStartOfDay(IST).toInstant(), end.plusDays(1).atStartOfDay(IST).toInstant());
        Map<Long, String> refs = payments.findAllById(rows.stream().map(LedgerEntry::getPaymentId)
                        .filter(id -> id != null).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Payment::getId, Payment::getReferenceNo, (a, b) -> a));
        return rows.stream().map(e -> {
            boolean out = "DEBIT".equals(e.getEntryType());
            return new StatementEntryDto(e.getId(), e.getCreatedAt(), e.getDescription(),
                    e.getPaymentId() == null ? null : refs.get(e.getPaymentId()), e.getEntryType(),
                    out ? e.getAmount() : null, out ? null : e.getAmount(), e.getBalanceAfter());
        }).toList();
    }

    // ---------------- security ----------------

    @Transactional(readOnly = true)
    public List<DeviceDto> devices(Long userId, String currentDeviceId) {
        User u = require(userId);
        return devices.findByUserIdOrderByLastUsedAtDesc(userId).stream()
                .map(d -> new DeviceDto(d.getId(), d.getDeviceLabel(), d.getAddedAt(), d.getLastUsedAt(),
                        d.getDeviceId().equals(currentDeviceId), d.getDeviceId().equals(u.getRegisteredDeviceId())))
                .toList();
    }

    @Transactional
    public void removeDevice(Long userId, Long deviceRowId, String currentDeviceId) {
        TrustedDevice d = devices.findById(deviceRowId).filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Device not found"));
        if (d.getDeviceId().equals(currentDeviceId)) {
            throw ApiException.badRequest("You cannot remove the device you are using right now");
        }
        devices.delete(d);
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryDto> logins(Long userId) {
        return history.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20)).stream()
                .map(h -> new LoginHistoryDto(h.getCreatedAt(), h.isSuccess(), h.getDetail(), h.getDeviceLabel(),
                        h.getIpAddress(), h.getLocation()))
                .toList();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void changePassword(Long userId, ChangePasswordRequest r) {
        User u = users.findByIdForUpdate(userId).orElseThrow();
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Current password is wrong");
        }
        KycValidator.strongPassword(r.newPassword(), u.getUsername());
        if (!r.newPassword().equals(r.confirmPassword())) throw ApiException.badRequest("New passwords do not match");
        if (encoder.matches(r.newPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("New password must be different from the current one");
        }
        u.setPasswordHash(encoder.encode(r.newPassword()));
        mail.notify(u.getEmail(), "Password changed", "Your net-banking password was changed. If this was not you, "
                + "contact the bank immediately.");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void changePin(Long userId, ChangePinRequest r) {
        User u = users.findByIdForUpdate(userId).orElseThrow();
        checkPin(u, r.currentPin());
        KycValidator.strongPin(r.newPin(), u.getDateOfBirth(), u.getPhone());
        if (!r.newPin().equals(r.confirmPin())) throw ApiException.badRequest("New PINs do not match");
        u.setPinHash(encoder.encode(r.newPin()));
        mail.notify(u.getEmail(), "Transaction PIN changed", "Your transaction PIN was changed. If this was not you, "
                + "contact the bank immediately.");
    }

    /** Daily totals used to enforce the bank's transfer limits. */
    public BigDecimal sentToday(Long userId, List<String> channels) {
        Instant since = LocalDate.now(IST).atStartOfDay(IST).toInstant();
        BigDecimal v = payments.sentSince(userId, since, channels);
        return v == null ? BigDecimal.ZERO : v;
    }
}
