package com.fraudshield.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudshield.fraud.event.AccountCommandEvent;
import com.fraudshield.fraud.event.FraudDecisionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Sends verdicts to the payment service over internal HTTP (no broker required). */
@Component
public class EventPublisher {
    private final RestClient client;
    private final ObjectMapper mapper;
    public EventPublisher(RestClient.Builder builder, ObjectMapper mapper,
                          @Value("${fraudshield.payment-service-url:http://payment-service:8081}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
        this.mapper = mapper;
    }
    public void publishDecision(FraudDecisionEvent event) { post("/internal/decision", event); }
    public void publishAccountCommand(AccountCommandEvent event) { post("/internal/account-command", event); }
    private void post(String path, Object body) {
        client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
    }
}
