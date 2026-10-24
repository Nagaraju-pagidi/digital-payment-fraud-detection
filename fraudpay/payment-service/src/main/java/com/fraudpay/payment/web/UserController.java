package com.fraudpay.payment.web;

import com.fraudpay.payment.dto.*;
import com.fraudpay.payment.service.AccountService;
import com.fraudpay.payment.service.PayeeService;
import com.fraudpay.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Logged-in customer: account, statement, payees, profile & security.
 * X-User-Id is set by the API Gateway from the session token (role CUSTOMER).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AccountService accounts;
    private final PayeeService payees;
    private final PaymentService payments;

    public UserController(AccountService accounts, PayeeService payees, PaymentService payments) {
        this.accounts = accounts;
        this.payees = payees;
        this.payments = payments;
    }

    @GetMapping("/me")
    public UserDto me(@RequestHeader("X-User-Id") Long userId) {
        return accounts.me(userId);
    }

    @GetMapping("/me/profile")
    public ProfileDto profile(@RequestHeader("X-User-Id") Long userId) {
        return accounts.profile(userId);
    }

    @GetMapping("/me/statement")
    public List<StatementEntryDto> statement(@RequestHeader("X-User-Id") Long userId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return accounts.statement(userId, from, to);
    }

    // ---------- security ----------

    @GetMapping("/me/devices")
    public List<DeviceDto> devices(@RequestHeader("X-User-Id") Long userId,
                                   @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return accounts.devices(userId, deviceId);
    }

    @DeleteMapping("/me/devices/{id}")
    public Map<String, String> removeDevice(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        accounts.removeDevice(userId, id, deviceId);
        return Map.of("message", "Device removed. It will need an OTP to log in again.");
    }

    @GetMapping("/me/logins")
    public List<LoginHistoryDto> logins(@RequestHeader("X-User-Id") Long userId) {
        return accounts.logins(userId);
    }

    @PostMapping("/me/password")
    public Map<String, String> changePassword(@RequestHeader("X-User-Id") Long userId,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        accounts.changePassword(userId, request);
        return Map.of("message", "Password changed successfully");
    }

    @PostMapping("/me/pin")
    public Map<String, String> changePin(@RequestHeader("X-User-Id") Long userId,
                                         @Valid @RequestBody ChangePinRequest request) {
        accounts.changePin(userId, request);
        return Map.of("message", "Transaction PIN changed successfully");
    }

    // ---------- payees ----------

    @GetMapping("/payees")
    public List<PayeeDto> payees(@RequestHeader("X-User-Id") Long userId) {
        return payees.list(userId);
    }

    @PostMapping("/payees/lookup")
    public Map<String, String> lookupPayee(@RequestHeader("X-User-Id") Long userId,
                                           @Valid @RequestBody PayeeLookupRequest request) {
        return payees.lookup(userId, request);
    }

    @PostMapping("/payees")
    public OtpChallengeResponse addPayee(@RequestHeader("X-User-Id") Long userId,
                                         @Valid @RequestBody AddPayeeRequest request) {
        return payees.startAdd(userId, request);
    }

    @PostMapping("/payees/verify")
    public PayeeDto verifyPayee(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody OtpVerifyRequest request) {
        return payees.completeAdd(userId, request);
    }

    @DeleteMapping("/payees/{id}")
    public Map<String, String> deletePayee(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        payees.delete(userId, id);
        return Map.of("message", "Payee deleted");
    }

    // ---------- UPI ----------

    @GetMapping("/upi/lookup")
    public Map<String, String> lookupUpi(@RequestParam String upiId) {
        return payments.lookupUpi(upiId);
    }
}
