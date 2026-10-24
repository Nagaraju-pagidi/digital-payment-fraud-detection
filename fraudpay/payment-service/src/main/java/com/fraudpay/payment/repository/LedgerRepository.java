package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDescIdDesc(Long userId, Instant from, Instant to);

}
