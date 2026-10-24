package com.fraudshield.fraud.repository;

import com.fraudshield.fraud.domain.AccountFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountFlagRepository extends JpaRepository<AccountFlag, Long> {
    List<AccountFlag> findByFrozenTrueOrderByFrozenAtDesc();

    long countByFrozenTrue();
}
