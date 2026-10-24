package com.fraudpay.payment.web;

import com.fraudpay.payment.event.AccountCommandEvent;
import com.fraudpay.payment.event.FraudDecisionEvent;
import com.fraudpay.payment.service.DecisionService;
import org.springframework.web.bind.annotation.*;

/** Internal callbacks from FraudShield; service should only be reachable on the Compose network. */
@RestController
@RequestMapping("/internal")
public class InternalDecisionController {
    private final DecisionService decisions;
    public InternalDecisionController(DecisionService decisions) { this.decisions = decisions; }
    @PostMapping("/decision") public void decision(@RequestBody FraudDecisionEvent event) { decisions.handleDecision(event); }
    @PostMapping("/account-command") public void command(@RequestBody AccountCommandEvent event) { decisions.handleAccountCommand(event); }
}
