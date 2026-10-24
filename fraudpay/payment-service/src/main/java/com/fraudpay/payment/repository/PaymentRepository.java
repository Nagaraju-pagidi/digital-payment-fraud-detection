package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.Payment;
import com.fraudpay.payment.domain.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Query("select p from Payment p where p.senderId = :userId or p.receiverId = :userId order by p.createdAt desc")
    List<Payment> findHistory(@Param("userId") Long userId, Pageable pageable);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant before);

    /** Money sent today and not refunded (for the daily transfer limit). */
    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.senderId = :userId and p.createdAt >= :since
              and p.status in (com.fraudpay.payment.domain.PaymentStatus.PROCESSING,
                               com.fraudpay.payment.domain.PaymentStatus.ON_HOLD,
                               com.fraudpay.payment.domain.PaymentStatus.SUCCESS)
              and p.channel in :channels
            """)
    BigDecimal sentSince(@Param("userId") Long userId, @Param("since") Instant since,
                         @Param("channels") List<String> channels);
}
