package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.LoginHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<LoginHistory> findByUserIdAndSuccessTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
