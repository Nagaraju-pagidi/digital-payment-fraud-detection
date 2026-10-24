package com.fraudshield.fraud.service;

import com.fraudshield.fraud.domain.CaseStatus;
import com.fraudshield.fraud.domain.FraudCase;
import com.fraudshield.fraud.dto.CaseDto;
import com.fraudshield.fraud.repository.FraudCaseRepository;
import com.fraudshield.fraud.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/** Human-in-the-loop: analysts approve or reject payments that are ON HOLD. */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final FraudCaseRepository cases;
    private final FraudAnalysisService analysis;
    private final EventPublisher publisher;
    private final AccountService accounts;
    private final LiveFeedService liveFeed;
    private final CaseMapper mapper;

    public ReviewService(FraudCaseRepository cases, FraudAnalysisService analysis, EventPublisher publisher,
                         AccountService accounts, LiveFeedService liveFeed, CaseMapper mapper) {
        this.cases = cases;
        this.analysis = analysis;
        this.publisher = publisher;
        this.accounts = accounts;
        this.liveFeed = liveFeed;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CaseDto> list(String status, int limit) {
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limit, 1), 500));
        List<FraudCase> rows = switch (status == null ? "ALL" : status.toUpperCase()) {
            case "HOLD" -> cases.findByStatusInOrderByCreatedAtDesc(EnumSet.of(CaseStatus.HOLD), page);
            case "FLAGGED" -> cases.findByStatusInOrderByCreatedAtDesc(
                    EnumSet.of(CaseStatus.HOLD, CaseStatus.BLOCKED, CaseStatus.ANALYST_APPROVED, CaseStatus.ANALYST_REJECTED), page);
            case "BLOCKED" -> cases.findByStatusInOrderByCreatedAtDesc(
                    EnumSet.of(CaseStatus.BLOCKED, CaseStatus.ANALYST_REJECTED), page);
            case "APPROVED" -> cases.findByStatusInOrderByCreatedAtDesc(
                    EnumSet.of(CaseStatus.APPROVED, CaseStatus.ANALYST_APPROVED), page);
            default -> cases.findAllByOrderByCreatedAtDesc(page);
        };
        return rows.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CaseDto get(Long transactionId) {
        return cases.findById(transactionId).map(mapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Case not found"));
    }

    @Transactional(readOnly = true)
    public List<CaseDto> customerHistory(Long senderId) {
        return cases.findBySenderIdOrderByCreatedAtDesc(senderId, PageRequest.of(0, 20)).stream()
                .map(mapper::toDto).toList();
    }

    @Transactional
    public CaseDto approve(Long transactionId, String analyst, String note) {
        FraudCase c = lockHeldCase(transactionId);
        c.review(CaseStatus.ANALYST_APPROVED, analyst, note);
        analysis.learn(c); // a genuine payment becomes part of the customer's normal behaviour
        publisher.publishDecision(analysis.toEvent(c, mapper.hits(c), "ANALYST", note));
        log.info("Analyst {} APPROVED payment #{}", analyst, transactionId);
        return done(c);
    }

    @Transactional
    public CaseDto reject(Long transactionId, String analyst, String note) {
        FraudCase c = lockHeldCase(transactionId);
        c.review(CaseStatus.ANALYST_REJECTED, analyst, note);
        publisher.publishDecision(analysis.toEvent(c, mapper.hits(c), "ANALYST", note));
        log.info("Analyst {} REJECTED payment #{}", analyst, transactionId);

        long bad = cases.countBySenderIdAndStatusInAndCreatedAtAfter(c.getSenderId(),
                List.of(CaseStatus.BLOCKED, CaseStatus.ANALYST_REJECTED), Instant.now().minus(Duration.ofHours(24)));
        if (bad >= 3 && accounts.freeze(c.getSenderId(), c.getSenderName(), c.getSenderEmail(), c.getSenderUpi(),
                bad + " blocked/rejected payments in 24 hours")) {
            c.setAccountFrozen(true);
        }
        return done(c);
    }

    private FraudCase lockHeldCase(Long transactionId) {
        FraudCase c = cases.findByIdForUpdate(transactionId)
                .orElseThrow(() -> ApiException.notFound("Case not found"));
        if (c.getStatus() != CaseStatus.HOLD) {
            throw ApiException.badRequest("Only payments ON HOLD can be reviewed (this one is " + c.getStatus() + ")");
        }
        return c;
    }

    private CaseDto done(FraudCase c) {
        CaseDto dto = mapper.toDto(c);
        AfterCommit.run(() -> liveFeed.broadcast("case", dto));
        return dto;
    }
}
