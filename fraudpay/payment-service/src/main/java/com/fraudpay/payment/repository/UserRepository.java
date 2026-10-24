package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Locks the account row so two payments can never spend the same money twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByUpiIdIgnoreCase(String upiId);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByCustomerIdIgnoreCase(String customerId);

    Optional<User> findByAccountNumber(String accountNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByPanIgnoreCase(String pan);

    boolean existsByAadhaarHash(String aadhaarHash);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByCustomerId(String customerId);

    List<User> findByBotTrueOrderByIdAsc();
}
