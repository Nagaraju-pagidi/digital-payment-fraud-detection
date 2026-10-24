package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtpChallenge o where o.id = :id")
    Optional<OtpChallenge> findByIdForUpdate(@Param("id") UUID id);
}
