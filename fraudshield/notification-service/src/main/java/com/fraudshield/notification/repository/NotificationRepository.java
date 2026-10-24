package com.fraudshield.notification.repository;

import com.fraudshield.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatusAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(String status, Instant before, Pageable pageable);

    List<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);
}
