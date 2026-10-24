package com.fraudshield.fraud.web;

import com.fraudshield.fraud.event.PaymentRequestedEvent;
import com.fraudshield.fraud.service.FraudAnalysisService;
import org.springframework.web.bind.annotation.*;

/** Internal service-to-service endpoint. Keep this service on the private Compose network. */
@RestController
@RequestMapping("/internal")
public class InternalAnalysisController {
    private final FraudAnalysisService analysis;
    public InternalAnalysisController(FraudAnalysisService analysis) { this.analysis = analysis; }
    @PostMapping("/analyze")
    public void analyze(@RequestBody PaymentRequestedEvent event) { analysis.analyze(event); }
}
