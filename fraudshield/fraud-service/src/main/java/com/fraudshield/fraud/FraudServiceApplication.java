package com.fraudshield.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FraudShield fraud-service (port 8082) - the middle layer.
 * Reads every "payment.requested" event, compares it with the customer's normal behaviour
 * using 12 rules, and publishes APPROVED / HOLD / BLOCKED on "fraud.decisions".
 * Also serves the Control Center (cases, analyst review, frozen accounts, live feed).
 */
@SpringBootApplication
@EnableScheduling
public class FraudServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudServiceApplication.class, args);
    }
}
