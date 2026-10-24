package com.fraudpay.payment.web;

import com.fraudpay.payment.dto.*;
import com.fraudpay.payment.service.LiveUpdateService;
import com.fraudpay.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final LiveUpdateService live;

    public PaymentController(PaymentService paymentService, LiveUpdateService live) {
        this.paymentService = paymentService;
        this.live = live;
    }

    /** Net banking step 1: payee + amount + IMPS/NEFT + transaction PIN -> OTP emailed. */
    @PostMapping("/transfer")
    public OtpChallengeResponse startTransfer(@RequestHeader("X-User-Id") Long userId,
                                              @Valid @RequestBody TransferRequest request) {
        return paymentService.startTransfer(userId, request);
    }

    /** Net banking step 2: OTP -> money held and sent to FraudShield. */
    @PostMapping("/transfer/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentDto confirmTransfer(@RequestHeader("X-User-Id") Long userId,
                                      @Valid @RequestBody OtpVerifyRequest request) {
        return paymentService.confirmTransfer(userId, request);
    }

    /** UPI: UPI ID + amount + transaction PIN. */
    @PostMapping("/upi")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentDto payUpi(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody UpiPaymentRequest request) {
        return paymentService.payUpi(userId, request);
    }

    @GetMapping
    public List<PaymentDto> history(@RequestHeader("X-User-Id") Long userId) {
        return paymentService.history(userId);
    }

    @GetMapping("/{id}")
    public PaymentDto get(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return paymentService.get(userId, id);
    }

    /** Live updates (Server-Sent Events) for this customer. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader("X-User-Id") Long userId) {
        return live.subscribe(userId);
    }
}
