package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "login_history")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private boolean success;
    private String detail;
    @Column(name = "device_id")
    private String deviceId;
    @Column(name = "device_label")
    private String deviceLabel;
    @Column(name = "ip_address")
    private String ipAddress;
    private String location;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected LoginHistory() {
    }

    public LoginHistory(Long userId, boolean success, String detail, String deviceId, String deviceLabel,
                        String ipAddress, String location) {
        this.userId = userId;
        this.success = success;
        this.detail = detail;
        this.deviceId = deviceId;
        this.deviceLabel = deviceLabel;
        this.ipAddress = ipAddress;
        this.location = location;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public boolean isSuccess() { return success; }
    public String getDetail() { return detail; }
    public String getDeviceId() { return deviceId; }
    public String getDeviceLabel() { return deviceLabel; }
    public String getIpAddress() { return ipAddress; }
    public String getLocation() { return location; }
    public Instant getCreatedAt() { return createdAt; }
}
