package com.fraudshield.fraud.repository;

import com.fraudshield.fraud.domain.Analyst;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalystRepository extends JpaRepository<Analyst, Long> {
    Optional<Analyst> findByUsernameIgnoreCase(String username);
}
