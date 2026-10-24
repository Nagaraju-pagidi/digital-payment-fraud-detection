package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByUserIdAndDeletedFalseOrderByNameAsc(Long userId);

    Optional<Beneficiary> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    boolean existsByUserIdAndAccountNumberAndDeletedFalse(Long userId, String accountNumber);
}
