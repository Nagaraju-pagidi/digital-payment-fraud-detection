package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {
    Optional<TrustedDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<TrustedDevice> findByUserIdOrderByLastUsedAtDesc(Long userId);
}
