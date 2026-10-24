package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.AccountType;
import com.fraudpay.payment.domain.LedgerEntry;
import com.fraudpay.payment.domain.LoginHistory;
import com.fraudpay.payment.domain.OtpChallenge;
import com.fraudpay.payment.domain.TrustedDevice;
import com.fraudpay.payment.domain.User;
import com.fraudpay.payment.dto.*;
import com.fraudpay.payment.geo.GeoCatalog;
import com.fraudpay.payment.repository.LedgerRepository;
import com.fraudpay.payment.repository.LoginHistoryRepository;
import com.fraudpay.payment.repository.TrustedDeviceRepository;
import com.fraudpay.payment.repository.UserRepository;
import com.fraudpay.payment.security.JwtService;
import com.fraudpay.payment.security.KycValidator;
import com.fraudpay.payment.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Account opening (KYC + email OTP) and secure login
 * (lockout after 5 wrong passwords, email OTP on a new device, login history).
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);


    private static final SecureRandom RANDOM = new SecureRandom();
    public static final List<String> GENDERS = List.of("MALE", "FEMALE", "OTHER");
    public static final List<String> OCCUPATIONS = List.of("SALARIED", "SELF_EMPLOYED", "BUSINESS", "STUDENT",
            "HOMEMAKER", "RETIRED", "OTHER");
    public static final List<String> INCOMES = List.of("BELOW_2.5L", "2.5L-5L", "5L-10L", "10L-25L", "ABOVE_25L");
    public static final List<String> RELATIONS = List.of("SPOUSE", "FATHER", "MOTHER", "SON", "DAUGHTER",
            "BROTHER", "SISTER", "OTHER");

    /** Everything needed to create the account once the email OTP is confirmed (no plain secrets inside). */
    public record PendingRegistration(String fullName, LocalDate dateOfBirth, String gender, String email,
                                      String phone, String pan, String aadhaarHash, String aadhaarLast4,
                                      String addressLine, String city, String state, String pincode,
                                      String occupation, String annualIncome, String accountType,
                                      String nomineeName, String nomineeRelation, String username,
                                      String passwordHash, String pinHash, String homeCity, String homeCountry,
                                      Double homeLat, Double homeLon, String deviceId, String deviceLabel) {
    }

    public record PendingLogin(String deviceId, String deviceLabel, String ip, String location, Double latitude, Double longitude) {
    }

    private final UserRepository users;
    private final TrustedDeviceRepository devices;
    private final LoginHistoryRepository history;
    private final LedgerRepository ledger;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final GeoCatalog geo;
    private final OtpService otp;
    private final MailService mail;
    private final BigDecimal openingBalance;
    private final String bankCode;
    private final int maxLoginFailures;
    private final long loginLockMinutes;
    private final RestClient mlClient;

    public AuthService(UserRepository users, TrustedDeviceRepository devices, LoginHistoryRepository history,
                       LedgerRepository ledger, PasswordEncoder encoder, JwtService jwt, GeoCatalog geo,
                       OtpService otp, MailService mail,
                       @Value("${fraudpay.opening-balance:50000}") BigDecimal openingBalance,
                       @Value("${fraudpay.bank-code:FRPY}") String bankCode,
                       @Value("${fraudpay.security.max-login-failures:5}") int maxLoginFailures,
                       @Value("${fraudpay.security.login-lock-minutes:15}") long loginLockMinutes,
                       RestClient.Builder restClientBuilder,
                       @Value("${fraudpay.ml-url:http://ml-fraud-service:8091}") String mlUrl) {
        this.users = users;
        this.devices = devices;
        this.history = history;
        this.ledger = ledger;
        this.encoder = encoder;
        this.jwt = jwt;
        this.geo = geo;
        this.otp = otp;
        this.mail = mail;
        this.openingBalance = openingBalance;
        this.bankCode = bankCode;
        this.maxLoginFailures = maxLoginFailures;
        this.loginLockMinutes = loginLockMinutes;
        this.mlClient = restClientBuilder.baseUrl(mlUrl).build();
    }

    // =====================================================================
    //  Account opening - step 1: validate everything, email an OTP
    // =====================================================================
    @Transactional
    public OtpChallengeResponse startRegistration(RegisterRequest r) {
        String name = KycValidator.name(r.fullName(), "Full name");
        KycValidator.adult(r.dateOfBirth());
        String gender = oneOf(r.gender(), GENDERS, "gender");
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        String phone = KycValidator.mobile(r.phone());
        String pan = KycValidator.pan(r.pan(), name);
        String aadhaar = KycValidator.aadhaar(r.aadhaar());
        String address = r.addressLine().trim();
        if (address.length() < 10) throw ApiException.badRequest("Please enter the full address (house no., street, area)");
        GeoCatalog.City city = geo.findHomeCity(r.city())
                .orElseThrow(() -> ApiException.badRequest("Please choose your city from the list"));
        String pincode = KycValidator.pincode(r.pincode());
        String occupation = oneOf(r.occupation(), OCCUPATIONS, "occupation");
        String income = oneOf(r.annualIncome(), INCOMES, "annual income");
        String accountType = oneOf(r.accountType(), List.of("SAVINGS", "CURRENT"), "account type");
        String nominee = null;
        String relation = null;
        if (r.nomineeName() != null && !r.nomineeName().isBlank()) {
            nominee = KycValidator.name(r.nomineeName(), "Nominee name");
            relation = oneOf(r.nomineeRelation(), RELATIONS, "nominee relation");
        }
        String username = KycValidator.username(r.username());
        KycValidator.strongPassword(r.password(), username);
        if (!r.password().equals(r.confirmPassword())) throw ApiException.badRequest("Passwords do not match");
        KycValidator.strongPin(r.pin(), r.dateOfBirth(), phone);
        if (!r.pin().equals(r.confirmPin())) throw ApiException.badRequest("Transaction PINs do not match");

        String aadhaarHash = KycValidator.sha256("FRAUDPAY-AADHAAR:" + aadhaar);
        checkUnique(email, phone, pan, aadhaarHash, username);

        // "Home" for fraud checks = where the customer is while opening the account
        String homeCity = city.name();
        String homeCountry = city.country();
        Double homeLat = city.lat();
        Double homeLon = city.lon();
        if (r.latitude() != null && r.longitude() != null) {
            GeoCatalog.GeoMatch here = geo.nearest(r.latitude(), r.longitude());
            homeCity = here.city();
            homeCountry = here.country();
            homeLat = r.latitude();
            homeLon = r.longitude();
        }

        PendingRegistration pending = new PendingRegistration(name, r.dateOfBirth(), gender, email, phone, pan,
                aadhaarHash, aadhaar.substring(8), address, city.name(), city.state(), pincode, occupation, income,
                accountType, nominee, relation, username, encoder.encode(r.password()), encoder.encode(r.pin()),
                homeCity, homeCountry, homeLat, homeLon, r.deviceId(), r.deviceLabel());
        return otp.start(null, OtpService.REGISTER, email, "opening your NexaPay account", pending);
    }

    // =====================================================================
    //  Account opening - step 2: correct OTP -> create customer + account
    // =====================================================================
    @Transactional
    public RegistrationResult completeRegistration(OtpVerifyRequest req) {
        OtpChallenge c = otp.verify(req.challengeId(), OtpService.REGISTER, null, req.otp());
        PendingRegistration p = otp.payload(c, PendingRegistration.class);
        checkUnique(p.email(), p.phone(), p.pan(), p.aadhaarHash(), p.username());

        User u = new User();
        u.setCustomerId(uniqueCustomerId());
        u.setUsername(p.username());
        u.setFullName(p.fullName());
        u.setDateOfBirth(p.dateOfBirth());
        u.setGender(p.gender());
        u.setEmail(p.email());
        u.setPhone(p.phone());
        u.setPan(p.pan());
        u.setAadhaarHash(p.aadhaarHash());
        u.setAadhaarLast4(p.aadhaarLast4());
        u.setAddressLine(p.addressLine());
        u.setCity(p.city());
        u.setState(p.state());
        u.setPincode(p.pincode());
        u.setOccupation(p.occupation());
        u.setAnnualIncome(p.annualIncome());
        u.setNomineeName(p.nomineeName());
        u.setNomineeRelation(p.nomineeRelation());
        u.setAccountType(AccountType.valueOf(p.accountType()));
        u.setAccountNumber(uniqueAccountNumber());
        u.setIfsc(ifscFor(p.city()));
        u.setBranch(p.city() + " Main Branch");
        u.setUpiId(p.username() + "@fraudpay");
        u.setPasswordHash(p.passwordHash());
        u.setPinHash(p.pinHash());
        u.setBalance(openingBalance);
        u.setHomeCity(p.homeCity());
        u.setHomeCountry(p.homeCountry());
        u.setHomeLat(p.homeLat());
        u.setHomeLon(p.homeLon());
        u.setRegisteredDeviceId(p.deviceId());
        users.save(u);

        devices.save(new TrustedDevice(u.getId(), p.deviceId(), p.deviceLabel()));
        ledger.save(new LedgerEntry(u.getId(), null, "OPENING", openingBalance, openingBalance,
                "Account opened - initial deposit (demo money)"));

        mail.notify(u.getEmail(), "Welcome - your account is ready", """
                Dear %s,

                Your %s account has been opened successfully.

                Customer ID    : %s
                User ID        : %s
                Account number : %s
                IFSC           : %s
                Branch         : %s
                UPI ID         : %s

                Login at the NexaPay net-banking site with your User ID and password.
                """.formatted(u.getFullName(), u.getAccountType().name().toLowerCase(Locale.ROOT), u.getCustomerId(),
                u.getUsername(), u.getAccountNumber(), u.getIfsc(), u.getBranch(), u.getUpiId()));

        log.info("Account opened: {} {} ({}), home {}", u.getCustomerId(), u.getUsername(), u.getAccountNumber(), u.getHomeCity());
        return new RegistrationResult(u.getCustomerId(), u.getUsername(), u.getFullName(), u.getAccountNumber(),
                u.getIfsc(), u.getBranch(), u.getUpiId(), u.getAccountType().name(), openingBalance);
    }

    // =====================================================================
    //  Login - step 1: User ID + password (and device check)
    // =====================================================================
    @Transactional(noRollbackFor = ApiException.class)   // failed attempts must be saved
    public LoginResponse login(LoginRequest r, String ip) {
        String id = r.userId().trim();
        Optional<User> found = id.toUpperCase(Locale.ROOT).startsWith("FP")
                ? users.findByCustomerIdIgnoreCase(id) : users.findByUsernameIgnoreCase(id);
        if (found.isEmpty()) found = users.findByUsernameIgnoreCase(id);
        User u = found.filter(x -> !x.isBot())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid User ID or password"));
        u = users.findByIdForUpdate(u.getId()).orElseThrow();

        String location = describe(r.latitude(), r.longitude());
        if (u.isLocked()) {
            history.save(new LoginHistory(u.getId(), false, "Blocked - login locked", r.deviceId(), r.deviceLabel(), ip, location));
            throw new ApiException(HttpStatus.LOCKED, "Your login is locked after " + maxLoginFailures
                    + " wrong passwords. Try again after " + time(u.getLockedUntil()) + ".");
        }
        if (!encoder.matches(r.password(), u.getPasswordHash())) {
            u.setFailedLogins(u.getFailedLogins() + 1);
            int left = maxLoginFailures - u.getFailedLogins();
            if (left <= 0) {
                u.setLockedUntil(Instant.now().plus(Duration.ofMinutes(loginLockMinutes)));
                u.setFailedLogins(0);
                history.save(new LoginHistory(u.getId(), false, "Wrong password - login locked", r.deviceId(), r.deviceLabel(), ip, location));
                mail.notify(u.getEmail(), "Login locked", "Your net-banking login was locked after " + maxLoginFailures
                        + " wrong password attempts (device: " + r.deviceLabel() + ", location: " + location
                        + "). If this was not you, please contact the bank.");
                throw new ApiException(HttpStatus.LOCKED, "Too many wrong passwords. Your login is locked for "
                        + loginLockMinutes + " minutes.");
            }
            history.save(new LoginHistory(u.getId(), false, "Wrong password", r.deviceId(), r.deviceLabel(), ip, location));
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Invalid User ID or password. " + left + " attempt(s) left before your login is locked.");
        }
        u.setFailedLogins(0);
        if (u.isFrozen()) {
            // still allowed to log in and see the account, but payments are blocked
            log.info("Frozen account {} logged in", u.getUsername());
        }

        // Risk-based step-up: a known device with implausible travel/unusual timing must verify by email OTP.
        int loginRisk = scoreLoginRisk(u.getId(), r, location);
        if (loginRisk >= 60) {
            history.save(new LoginHistory(u.getId(), false, "Risk-based OTP required (risk " + loginRisk + ")",
                    r.deviceId(), r.deviceLabel(), ip, location));
            OtpChallengeResponse ch = otp.start(u.getId(), OtpService.LOGIN, u.getEmail(),
                    "unusual login location/time - additional verification required",
                    new PendingLogin(r.deviceId(), r.deviceLabel(), ip, location, r.latitude(), r.longitude()));
            return LoginResponse.otp(ch);
        }

        // Demo customers created by the seeder: the first login binds device + "home" (like a first-time app setup)
        if (u.getRegisteredDeviceId() == null) {
            u.setRegisteredDeviceId(r.deviceId());
            if (r.latitude() != null && r.longitude() != null) {
                GeoCatalog.GeoMatch here = geo.nearest(r.latitude(), r.longitude());
                u.setHomeLat(r.latitude());
                u.setHomeLon(r.longitude());
                u.setHomeCity(here.city());
                u.setHomeCountry(here.country());
            }
            devices.save(new TrustedDevice(u.getId(), r.deviceId(), r.deviceLabel()));
            return finishLogin(u, r.deviceId(), r.deviceLabel(), ip, location, r.latitude(), r.longitude(), "First login - device registered");
        }

        Optional<TrustedDevice> trusted = devices.findByUserIdAndDeviceId(u.getId(), r.deviceId());
        if (trusted.isPresent()) {
            trusted.get().touch();
            return finishLogin(u, r.deviceId(), r.deviceLabel(), ip, location, r.latitude(), r.longitude(), "Login");
        }

        // Unknown device -> second factor by email
        history.save(new LoginHistory(u.getId(), false, "New device - OTP sent", r.deviceId(), r.deviceLabel(), ip, location));
        OtpChallengeResponse ch = otp.start(u.getId(), OtpService.LOGIN, u.getEmail(),
                "login from a new device (" + r.deviceLabel() + ", " + location + ")",
                new PendingLogin(r.deviceId(), r.deviceLabel(), ip, location, r.latitude(), r.longitude()));
        return LoginResponse.otp(ch);
    }

    // =====================================================================
    //  Login - step 2 (new device only): email OTP
    // =====================================================================
    @Transactional
    public LoginResponse completeLogin(OtpVerifyRequest req) {
        OtpChallenge c = otp.verify(req.challengeId(), OtpService.LOGIN, null, req.otp());
        PendingLogin p = otp.payload(c, PendingLogin.class);
        User u = users.findByIdForUpdate(c.getUserId()).orElseThrow();
        if (req.trustDevice() && devices.findByUserIdAndDeviceId(u.getId(), p.deviceId()).isEmpty()) {
            devices.save(new TrustedDevice(u.getId(), p.deviceId(), p.deviceLabel()));
        }
        return finishLogin(u, p.deviceId(), p.deviceLabel(), p.ip(), p.location(), p.latitude(), p.longitude(),
                req.trustDevice() ? "New device verified by OTP and trusted" : "New device verified by OTP");
    }

    @Transactional
    public OtpChallengeResponse resendOtp(String challengeId) {
        return otp.resend(challengeId, "your NexaPay request");
    }

    private int scoreLoginRisk(Long userId, LoginRequest request, String location) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("customer_id", String.valueOf(userId));
            event.put("occurred_at", Instant.now().toString());
            event.put("latitude", request.latitude());
            event.put("longitude", request.longitude());
            event.put("city", location);
            event.put("device_id", request.deviceId());
            java.util.Map<?, ?> response = mlClient.post().uri("/score-login")
                    .contentType(MediaType.APPLICATION_JSON).body(event).retrieve().body(java.util.Map.class);
            Object score = response == null ? null : response.get("riskScore");
            return score instanceof Number n ? n.intValue() : 0;
        } catch (Exception ex) {
            log.warn("ML login risk check unavailable for customer {}: {}", userId, ex.getMessage());
            return 0; // existing device and OTP controls remain active if ML is unavailable
        }
    }

    private LoginResponse finishLogin(User u, String deviceId, String deviceLabel, String ip, String location,
                                      Double latitude, Double longitude, String detail) {
        LastLoginDto previous = history.findByUserIdAndSuccessTrueOrderByCreatedAtDesc(u.getId(), PageRequest.of(0, 1))
                .stream().findFirst()
                .map(h -> new LastLoginDto(h.getCreatedAt(), h.getDeviceLabel(), h.getLocation(), h.getIpAddress()))
                .orElse(null);
        Instant loginAt = Instant.now();
        history.save(new LoginHistory(u.getId(), true, detail, deviceId, deviceLabel, ip, location));
        u.setLastLoginAt(loginAt);
        // Feed successful login context to the same per-customer behaviour history used for payments.
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("customer_id", String.valueOf(u.getId()));
            event.put("event_type", "LOGIN");
            event.put("occurred_at", loginAt.toString());
            event.put("latitude", latitude);
            event.put("longitude", longitude);
            event.put("city", location);
            event.put("device_id", deviceId);
            event.put("outcome", "APPROVED");
            mlClient.post().uri("/feedback").contentType(MediaType.APPLICATION_JSON).body(event).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Could not record ML login context for customer {}: {}", u.getId(), ex.getMessage());
        }
        return LoginResponse.ok(jwt.issueToken(u), Mapper.toDto(u), previous);
    }

    // =====================================================================
    //  helpers
    // =====================================================================
    private void checkUnique(String email, String phone, String pan, String aadhaarHash, String username) {
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.badRequest("An account with this email already exists");
        if (users.existsByPhone(phone)) throw ApiException.badRequest("An account with this mobile number already exists");
        if (users.existsByPanIgnoreCase(pan)) throw ApiException.badRequest("An account with this PAN already exists");
        if (users.existsByAadhaarHash(aadhaarHash)) throw ApiException.badRequest("An account with this Aadhaar already exists");
        if (users.existsByUsernameIgnoreCase(username)) throw ApiException.badRequest("This User ID is taken - please choose another");
    }

    private static String oneOf(String value, List<String> allowed, String label) {
        String v = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(v)) throw ApiException.badRequest("Please select a valid " + label);
        return v;
    }

    private String describe(Double lat, Double lon) {
        if (lat == null || lon == null) return "Location not shared";
        GeoCatalog.GeoMatch m = geo.nearest(lat, lon);
        return m.city() + ", " + m.country();
    }

    private static String time(Instant t) {
        return DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.of("Asia/Kolkata")).format(t);
    }

    String uniqueCustomerId() {
        String id;
        do {
            id = "FP" + (1000000 + RANDOM.nextInt(9000000));
        } while (users.existsByCustomerId(id));
        return id;
    }

    String uniqueAccountNumber() {
        String n;
        do {
            n = "5010" + String.format("%08d", RANDOM.nextInt(100_000_000));
        } while (users.existsByAccountNumber(n));
        return n;
    }

    /** FRPY0 + first 3 letters of the city + 001  ->  FRPY0HYD001 */
    String ifscFor(String city) {
        String letters = city.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        return bankCode + "0" + (letters + "XXX").substring(0, 3) + "001";
    }
}
