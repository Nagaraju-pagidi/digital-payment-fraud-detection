package com.fraudpay.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FraudPay payment-service (port 8081).
 * Plays the role of the bank / UPI app backend:
 * customers, wallets, PIN-protected transfers, and the payment saga.
 * Money is HELD until FraudShield (the middle layer) approves it.
 */
@SpringBootApplication
@EnableScheduling
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
