package com.fraudshield.fraud.service;

import com.fraudshield.fraud.domain.AccountFlag;
import com.fraudshield.fraud.dto.AccountFlagDto;
import com.fraudshield.fraud.event.AccountCommandEvent;
import com.fraudshield.fraud.repository.AccountFlagRepository;
import com.fraudshield.fraud.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Freezing and unfreezing customer accounts. FraudPay applies it via the "account.commands" topic. */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountFlagRepository flags;
    private final EventPublisher publisher;
    private final LiveFeedService liveFeed;
    private final CaseMapper mapper;

    public AccountService(AccountFlagRepository flags, EventPublisher publisher,
                          LiveFeedService liveFeed, CaseMapper mapper) {
        this.flags = flags;
        this.publisher = publisher;
        this.liveFeed = liveFeed;
        this.mapper = mapper;
    }

    public boolean isFrozen(Long userId) {
        return flags.findById(userId).map(AccountFlag::isFrozen).orElse(false);
    }

    /** Must be called inside a transaction. Returns false if the account was already frozen. */
    @Transactional
    public boolean freeze(Long userId, String name, String email, String upi, String reason) {
        AccountFlag flag = flags.findById(userId).orElseGet(() -> new AccountFlag(userId));
        if (flag.isFrozen()) return false;
        flag.freeze(name, email, upi, reason);
        flags.save(flag);
        publisher.publishAccountCommand(new AccountCommandEvent(UUID.randomUUID().toString(), userId, name, email,
                "FREEZE", reason, Instant.now()));
        log.warn("FROZEN account {} ({}): {}", upi, userId, reason);
        AccountFlagDto dto = mapper.toDto(flag);
        AfterCommit.run(() -> liveFeed.broadcast("account", dto));
        return true;
    }

    @Transactional
    public AccountFlagDto unfreeze(Long userId, String analyst) {
        AccountFlag flag = flags.findById(userId)
                .filter(AccountFlag::isFrozen)
                .orElseThrow(() -> ApiException.notFound("This account is not frozen"));
        flag.unfreeze(analyst);
        publisher.publishAccountCommand(new AccountCommandEvent(UUID.randomUUID().toString(), userId,
                flag.getUserName(), flag.getUserEmail(), "UNFREEZE", "Unfrozen by analyst " + analyst, Instant.now()));
        log.info("Account {} unfrozen by {}", flag.getUserUpi(), analyst);
        AccountFlagDto dto = mapper.toDto(flag);
        AfterCommit.run(() -> liveFeed.broadcast("account", dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<AccountFlagDto> frozenAccounts() {
        return flags.findByFrozenTrueOrderByFrozenAtDesc().stream().map(mapper::toDto).toList();
    }
}
