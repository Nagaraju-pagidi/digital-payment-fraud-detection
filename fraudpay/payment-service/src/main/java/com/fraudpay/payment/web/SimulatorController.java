package com.fraudpay.payment.web;

import com.fraudpay.payment.service.SimulatorService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Demo traffic generator - only FraudShield analysts can use it (checked by the gateway). */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulatorService simulator;

    public SimulatorController(SimulatorService simulator) {
        this.simulator = simulator;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return simulator.status();
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        return simulator.start();
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        return simulator.stop();
    }
}
