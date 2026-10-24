package com.fraudshield.fraud.repository;

import com.fraudshield.fraud.domain.CaseStatus;
import com.fraudshield.fraud.domain.FraudCase;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FraudCase c where c.transactionId = :id")
    Optional<FraudCase> findByIdForUpdate(@Param("id") Long id);

    List<FraudCase> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<FraudCase> findByStatusInOrderByCreatedAtDesc(Collection<CaseStatus> statuses, Pageable pageable);

    List<FraudCase> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    /** Used by the velocity rule: payments by this customer in the last few minutes. */
    long countBySenderIdAndCreatedAtAfter(Long senderId, Instant after);

    /** Used for the freeze rule: blocked/rejected payments in the last 24 hours. */
    long countBySenderIdAndStatusInAndCreatedAtAfter(Long senderId, Collection<CaseStatus> statuses, Instant after);
}
