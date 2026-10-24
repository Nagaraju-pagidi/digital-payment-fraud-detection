package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** A browser/phone the customer has verified (registration device, or later with an email OTP). */
@Entity
@Table(name = "trusted_devices")
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "device_id", nullable = false)
    private String deviceId;
    @Column(name = "device_label")
    private String deviceLabel;
    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected TrustedDevice() {
    }

    public TrustedDevice(Long userId, String deviceId, String deviceLabel) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceLabel = deviceLabel;
        this.lastUsedAt = Instant.now();
    }

    public void touch() {
        this.lastUsedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getDeviceLabel() { return deviceLabel; }
    public Instant getAddedAt() { return addedAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}
