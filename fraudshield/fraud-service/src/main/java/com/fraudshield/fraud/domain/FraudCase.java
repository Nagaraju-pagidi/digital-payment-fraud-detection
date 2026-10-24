package com.fraudshield.fraud.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** The fraud engine's record of one payment: what happened, the score and the verdict. */
@Entity
@Table(name = "fraud_cases")
public class FraudCase {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    private String channel;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(name = "sender_name", nullable = false)
    private String senderName;
    @Column(name = "sender_email")
    private String senderEmail;
    @Column(name = "sender_upi", nullable = false)
    private String senderUpi;
    @Column(name = "receiver_name")
    private String receiverName;
    @Column(name = "receiver_upi", nullable = false)
    private String receiverUpi;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    private String note;

    @Column(name = "device_id")
    private String deviceId;
    @Column(name = "device_label")
    private String deviceLabel;
    @Column(name = "new_device", nullable = false)
    private boolean newDevice;

    private Double latitude;
    private Double longitude;
    @Column(name = "location_city")
    private String locationCity;
    @Column(name = "location_country")
    private String locationCountry;
    @Column(name = "home_city")
    private String homeCity;
    @Column(name = "home_country")
    private String homeCountry;
    @Column(name = "distance_from_home_km")
    private Double distanceFromHomeKm;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private String decision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    /** JSON array of RuleHit */
    @Column(name = "rule_hits", nullable = false)
    private String ruleHits;

    @Column(nullable = false)
    private boolean simulated;

    @Column(name = "account_frozen", nullable = false)
    private boolean accountFrozen;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt = Instant.now();

    @Column(name = "reviewed_by")
    private String reviewedBy;
    @Column(name = "review_note")
    private String reviewNote;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public void review(CaseStatus newStatus, String analyst, String note) {
        this.status = newStatus;
        this.reviewedBy = analyst;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getSenderUpi() { return senderUpi; }
    public void setSenderUpi(String senderUpi) { this.senderUpi = senderUpi; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverUpi() { return receiverUpi; }
    public void setReceiverUpi(String receiverUpi) { this.receiverUpi = receiverUpi; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
    public boolean isNewDevice() { return newDevice; }
    public void setNewDevice(boolean newDevice) { this.newDevice = newDevice; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }
    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }
    public String getHomeCity() { return homeCity; }
    public void setHomeCity(String homeCity) { this.homeCity = homeCity; }
    public String getHomeCountry() { return homeCountry; }
    public void setHomeCountry(String homeCountry) { this.homeCountry = homeCountry; }
    public Double getDistanceFromHomeKm() { return distanceFromHomeKm; }
    public void setDistanceFromHomeKm(Double distanceFromHomeKm) { this.distanceFromHomeKm = distanceFromHomeKm; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public String getRuleHits() { return ruleHits; }
    public void setRuleHits(String ruleHits) { this.ruleHits = ruleHits; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }
    public boolean isAccountFrozen() { return accountFrozen; }
    public void setAccountFrozen(boolean accountFrozen) { this.accountFrozen = accountFrozen; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public Instant getReviewedAt() { return reviewedAt; }
}
