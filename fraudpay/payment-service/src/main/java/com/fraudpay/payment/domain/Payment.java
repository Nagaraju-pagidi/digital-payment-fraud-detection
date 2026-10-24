package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** One money transfer between two FraudPay users. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_no", nullable = false, unique = true)
    private String referenceNo;

    @Column(nullable = false)
    private String channel;   // IMPS | NEFT | UPI

    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(name = "sender_name", nullable = false)
    private String senderName;
    @Column(name = "sender_upi", nullable = false)
    private String senderUpi;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;
    @Column(name = "receiver_name", nullable = false)
    private String receiverName;
    @Column(name = "receiver_upi", nullable = false)
    private String receiverUpi;
    @Column(name = "receiver_account", nullable = false)
    private String receiverAccount;
    @Column(name = "payee_added_at")
    private Instant payeeAddedAt;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PROCESSING;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "fraud_score")
    private Integer fraudScore;

    /** Reasons from FraudShield, separated by " | " */
    @Column(name = "fraud_reasons")
    private String fraudReasons;

    @Column(name = "device_id")
    private String deviceId;
    @Column(name = "device_label")
    private String deviceLabel;

    private Double latitude;
    private Double longitude;
    @Column(name = "location_city")
    private String locationCity;
    @Column(name = "location_country")
    private String locationCountry;

    @Column(nullable = false)
    private boolean simulated;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @Column(name = "completed_at")
    private Instant completedAt;

    public void changeStatus(PaymentStatus newStatus, String message) {
        this.status = newStatus;
        this.statusMessage = message;
        this.updatedAt = Instant.now();
        if (!newStatus.isOpen()) {
            this.completedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
    public Instant getPayeeAddedAt() { return payeeAddedAt; }
    public void setPayeeAddedAt(Instant payeeAddedAt) { this.payeeAddedAt = payeeAddedAt; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderUpi() { return senderUpi; }
    public void setSenderUpi(String senderUpi) { this.senderUpi = senderUpi; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverUpi() { return receiverUpi; }
    public void setReceiverUpi(String receiverUpi) { this.receiverUpi = receiverUpi; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public PaymentStatus getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public Integer getFraudScore() { return fraudScore; }
    public void setFraudScore(Integer fraudScore) { this.fraudScore = fraudScore; }
    public String getFraudReasons() { return fraudReasons; }
    public void setFraudReasons(String fraudReasons) { this.fraudReasons = fraudReasons; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }
    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
