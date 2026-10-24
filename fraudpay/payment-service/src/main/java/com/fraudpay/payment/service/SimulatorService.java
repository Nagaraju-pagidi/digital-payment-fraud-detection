package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.User;
import com.fraudpay.payment.geo.GeoCatalog;
import com.fraudpay.payment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background traffic generator for the demo (started from the Control Center).
 * Simulator accounts ("bots") make normal everyday payments from their home city,
 * and occasionally a suspicious one (unusual amount / foreign location) - so the live
 * dashboard always has something to show. Real customers' wallets are never touched.
 */
@Service
public class SimulatorService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);
    private static final String[] NOTES = {"Groceries", "Dinner", "Rent share", "Movie tickets", "Electricity bill",
            "Petrol", "Books", "Medicines", "Gift", "Cab fare", "Tea stall", "Mobile recharge"};

    private final UserRepository users;
    private final PaymentService paymentService;
    private final GeoCatalog geo;
    private final TransactionTemplate tx;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger generated = new AtomicInteger();

    public SimulatorService(UserRepository users, PaymentService paymentService, GeoCatalog geo,
                            TransactionTemplate tx) {
        this.users = users;
        this.tx = tx;
        this.paymentService = paymentService;
        this.geo = geo;
    }

    public Map<String, Object> start() {
        running.set(true);
        log.info("Traffic simulator STARTED");
        return status();
    }

    public Map<String, Object> stop() {
        running.set(false);
        log.info("Traffic simulator STOPPED");
        return status();
    }

    public Map<String, Object> status() {
        return Map.of("running", running.get(), "generated", generated.get());
    }

    @Scheduled(fixedDelay = 2500)
    public void tick() {
        if (!running.get()) return;
        try {
            tx.executeWithoutResult(status -> generateOne());
        } catch (Exception e) {
            log.debug("Simulator skipped one payment: {}", e.getMessage());
        }
    }

    private void generateOne() {
        List<User> bots = users.findByBotTrueOrderByIdAsc();
        if (bots.size() < 2) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        User pick = bots.get(rnd.nextInt(bots.size()));
        User sender = users.findByIdForUpdate(pick.getId()).orElseThrow();
        if (sender.isFrozen()) return;
        User receiver;
        do {
            receiver = bots.get(rnd.nextInt(bots.size()));
        } while (receiver.getId().equals(sender.getId()));

        // keep the bots' wallets topped up ("salary credited")
        if (sender.getBalance().compareTo(BigDecimal.valueOf(20000)) < 0) {
            sender.credit(BigDecimal.valueOf(50000));
        }

        double lat = sender.getHomeLat() + rnd.nextDouble(-0.05, 0.05);
        double lon = sender.getHomeLon() + rnd.nextDouble(-0.05, 0.05);
        long amount = 100 + rnd.nextLong(2900);

        int dice = rnd.nextInt(100);
        if (dice < 3) {
            // suspicious: payment from a foreign city
            GeoCatalog.City far = geo.foreignCities().get(rnd.nextInt(geo.foreignCities().size()));
            lat = far.lat();
            lon = far.lon();
        } else if (dice < 10) {
            // suspicious: amount far above this bot's usual spending
            amount = 15000 + rnd.nextLong(15000);
        }

        paymentService.createPayment(sender, receiver, BigDecimal.valueOf(amount),
                NOTES[rnd.nextInt(NOTES.length)], "UPI", null, sender.getRegisteredDeviceId(), "Android - FraudPay app",
                lat, lon, true);
        generated.incrementAndGet();
    }
}
