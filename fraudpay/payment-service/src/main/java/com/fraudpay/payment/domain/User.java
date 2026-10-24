package com.fraudpay.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A NexaPay customer: KYC profile, account, balance and security state. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    @Column(nullable = false)
    private String gender;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false, unique = true)
    private String phone;
    @Column(nullable = false, unique = true)
    private String pan;
    @Column(name = "aadhaar_hash", nullable = false, unique = true)
    private String aadhaarHash;
    @Column(name = "aadhaar_last4", nullable = false)
    private String aadhaarLast4;

    @Column(name = "address_line", nullable = false)
    private String addressLine;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String state;
    @Column(nullable = false)
    private String pincode;
    @Column(nullable = false)
    private String occupation;
    @Column(name = "annual_income", nullable = false)
    private String annualIncome;
    @Column(name = "nominee_name")
    private String nomineeName;
    @Column(name = "nominee_relation")
    private String nomineeRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;
    @Column(nullable = false)
    private String ifsc;
    @Column(nullable = false)
    private String branch;
    @Column(name = "upi_id", nullable = false, unique = true)
    private String upiId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "home_city")
    private String homeCity;
    @Column(name = "home_country")
    private String homeCountry;
    @Column(name = "home_lat")
    private Double homeLat;
    @Column(name = "home_lon")
    private Double homeLon;
    @Column(name = "registered_device_id")
    private String registeredDeviceId;

    @Column(name = "failed_logins", nullable = false)
    private int failedLogins;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "pin_failures", nullable = false)
    private int pinFailures;
    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(nullable = false)
    private boolean bot;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // ---------- wallet operations ----------

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public boolean isFrozen() {
        return status == UserStatus.FROZEN;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean isPinLocked() {
        return pinLockedUntil != null && pinLockedUntil.isAfter(Instant.now());
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getAadhaarHash() { return aadhaarHash; }
    public void setAadhaarHash(String aadhaarHash) { this.aadhaarHash = aadhaarHash; }
    public String getAadhaarLast4() { return aadhaarLast4; }
    public void setAadhaarLast4(String aadhaarLast4) { this.aadhaarLast4 = aadhaarLast4; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(String annualIncome) { this.annualIncome = annualIncome; }
    public String getNomineeName() { return nomineeName; }
    public void setNomineeName(String nomineeName) { this.nomineeName = nomineeName; }
    public String getNomineeRelation() { return nomineeRelation; }
    public void setNomineeRelation(String nomineeRelation) { this.nomineeRelation = nomineeRelation; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }
    public String getHomeCity() { return homeCity; }
    public void setHomeCity(String homeCity) { this.homeCity = homeCity; }
    public String getHomeCountry() { return homeCountry; }
    public void setHomeCountry(String homeCountry) { this.homeCountry = homeCountry; }
    public Double getHomeLat() { return homeLat; }
    public void setHomeLat(Double homeLat) { this.homeLat = homeLat; }
    public Double getHomeLon() { return homeLon; }
    public void setHomeLon(Double homeLon) { this.homeLon = homeLon; }
    public String getRegisteredDeviceId() { return registeredDeviceId; }
    public void setRegisteredDeviceId(String registeredDeviceId) { this.registeredDeviceId = registeredDeviceId; }
    public int getFailedLogins() { return failedLogins; }
    public void setFailedLogins(int failedLogins) { this.failedLogins = failedLogins; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public int getPinFailures() { return pinFailures; }
    public void setPinFailures(int pinFailures) { this.pinFailures = pinFailures; }
    public Instant getPinLockedUntil() { return pinLockedUntil; }
    public void setPinLockedUntil(Instant pinLockedUntil) { this.pinLockedUntil = pinLockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public boolean isBot() { return bot; }
    public void setBot(boolean bot) { this.bot = bot; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
