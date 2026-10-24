package com.fraudshield.fraud.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "analysts")
public class Analyst {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Analyst() {
    }

    public Analyst(String username, String fullName, String passwordHash) {
        this.username = username;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
