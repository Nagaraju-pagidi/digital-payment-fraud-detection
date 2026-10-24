package com.fraudpay.payment.repository;

import com.fraudpay.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
