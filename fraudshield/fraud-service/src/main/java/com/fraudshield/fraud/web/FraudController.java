package com.fraudshield.fraud.web;

import com.fraudshield.fraud.dto.AccountFlagDto;
import com.fraudshield.fraud.dto.CaseDto;
import com.fraudshield.fraud.dto.ReviewRequest;
import com.fraudshield.fraud.dto.RuleDto;
import com.fraudshield.fraud.rules.RuleEngine;
import com.fraudshield.fraud.service.AccountService;
import com.fraudshield.fraud.service.LiveFeedService;
import com.fraudshield.fraud.service.ReviewService;
import com.fraudshield.fraud.service.StatsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Control Center API (role ANALYST - enforced by the API Gateway).
 * X-User-Name / X-User-Id are set by the gateway from the analyst's token.
 */
@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final ReviewService reviews;
    private final AccountService accounts;
    private final StatsService stats;
    private final LiveFeedService liveFeed;
    private final RuleEngine engine;

    public FraudController(ReviewService reviews, AccountService accounts, StatsService stats,
                           LiveFeedService liveFeed, RuleEngine engine) {
        this.reviews = reviews;
        this.accounts = accounts;
        this.stats = stats;
        this.liveFeed = liveFeed;
        this.engine = engine;
    }

    /** status = ALL | HOLD | FLAGGED | BLOCKED | APPROVED */
    @GetMapping("/cases")
    public List<CaseDto> cases(@RequestParam(defaultValue = "ALL") String status,
                               @RequestParam(defaultValue = "100") int limit) {
        return reviews.list(status, limit);
    }

    @GetMapping("/cases/{id}")
    public CaseDto caseDetail(@PathVariable Long id) {
        return reviews.get(id);
    }

    @GetMapping("/customers/{senderId}/cases")
    public List<CaseDto> customerHistory(@PathVariable Long senderId) {
        return reviews.customerHistory(senderId);
    }

    @PostMapping("/cases/{id}/approve")
    public CaseDto approve(@PathVariable Long id, @RequestBody(required = false) ReviewRequest body,
                           @RequestHeader(value = "X-User-Name", defaultValue = "analyst") String analyst) {
        return reviews.approve(id, analyst, body == null ? null : body.note());
    }

    @PostMapping("/cases/{id}/reject")
    public CaseDto reject(@PathVariable Long id, @RequestBody(required = false) ReviewRequest body,
                          @RequestHeader(value = "X-User-Name", defaultValue = "analyst") String analyst) {
        return reviews.reject(id, analyst, body == null ? null : body.note());
    }

    @GetMapping("/accounts/frozen")
    public List<AccountFlagDto> frozen() {
        return accounts.frozenAccounts();
    }

    @PostMapping("/accounts/{userId}/unfreeze")
    public AccountFlagDto unfreeze(@PathVariable Long userId,
                                   @RequestHeader(value = "X-User-Name", defaultValue = "analyst") String analyst) {
        return accounts.unfreeze(userId, analyst);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return stats.stats();
    }

    @GetMapping("/rules")
    public Map<String, Object> rules() {
        List<RuleDto> list = engine.rules().stream()
                .map(r -> new RuleDto(r.id(), r.name(), r.description(), r.maxPoints()))
                .toList();
        return Map.of("rules", list, "holdThreshold", engine.holdThreshold(), "blockThreshold", engine.blockThreshold());
    }

    /** Live feed of cases (Server-Sent Events). */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return liveFeed.subscribe();
    }
}
