package com.fraudpay.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudpay.payment.domain.OtpChallenge;
import com.fraudpay.payment.dto.OtpChallengeResponse;
import com.fraudpay.payment.repository.OtpChallengeRepository;
import com.fraudpay.payment.security.KycValidator;
import com.fraudpay.payment.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * One-time passwords (second factor).
 *  - 6 random digits, emailed to the customer's registered email
 *  - only the SHA-256 hash is stored, valid 5 minutes, 3 wrong tries allowed, single use
 *  - the "payload" remembers what to do after a correct OTP (open account, trust device, add payee, transfer)
 */
@Service
public class OtpService {

    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String ADD_PAYEE = "ADD_PAYEE";
    public static final String TRANSFER = "TRANSFER";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeRepository repo;
    private final MailService mail;
    private final ObjectMapper objectMapper;
    private final long validitySeconds;
    private final int maxAttempts;
    private final int maxResends;

    public OtpService(OtpChallengeRepository repo, MailService mail, ObjectMapper objectMapper,
                      @Value("${fraudpay.otp.validity-seconds:300}") long validitySeconds,
                      @Value("${fraudpay.otp.max-attempts:3}") int maxAttempts,
                      @Value("${fraudpay.otp.max-resends:3}") int maxResends) {
        this.repo = repo;
        this.mail = mail;
        this.objectMapper = objectMapper;
        this.validitySeconds = validitySeconds;
        this.maxAttempts = maxAttempts;
        this.maxResends = maxResends;
    }

    @Transactional
    public OtpChallengeResponse start(Long userId, String purpose, String email, String forWhat, Object payload) {
        String code = newCode();
        String json;
        try {
            json = payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot store OTP payload", e);
        }
        OtpChallenge c = new OtpChallenge(UUID.randomUUID(), userId, purpose, email, KycValidator.sha256(code), json,
                Instant.now().plusSeconds(validitySeconds));
        repo.save(c);
        mail.send(email, "OTP " + code + " for " + forWhat, message(code, forWhat));
        return new OtpChallengeResponse(c.getId().toString(), KycValidator.maskEmail(email), validitySeconds, purpose);
    }

    @Transactional
    public OtpChallengeResponse resend(String challengeId, String forWhat) {
        OtpChallenge c = load(challengeId);
        if (c.isUsed()) throw ApiException.badRequest("This request is already completed");
        if (c.getResends() >= maxResends) throw ApiException.badRequest("OTP resend limit reached. Please start again");
        String code = newCode();
        c.setCodeHash(KycValidator.sha256(code));
        c.setResends(c.getResends() + 1);
        c.setAttempts(0);
        c.setExpiresAt(Instant.now().plusSeconds(validitySeconds));
        mail.send(c.getEmail(), "OTP " + code + " for " + forWhat, message(code, forWhat));
        return new OtpChallengeResponse(c.getId().toString(), KycValidator.maskEmail(c.getEmail()), validitySeconds, c.getPurpose());
    }

    /**
     * Checks the OTP. A wrong attempt is saved even though an error is returned
     * (noRollbackFor), so the 3-attempt limit cannot be bypassed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public OtpChallenge verify(String challengeId, String purpose, Long expectedUserId, String code) {
        OtpChallenge c = repo.findByIdForUpdate(parse(challengeId))
                .orElseThrow(() -> ApiException.badRequest("OTP request not found - please start again"));
        if (!c.getPurpose().equals(purpose) || (expectedUserId != null && !expectedUserId.equals(c.getUserId()))) {
            throw ApiException.badRequest("OTP request not valid for this action");
        }
        if (c.isUsed()) throw ApiException.badRequest("This OTP was already used");
        if (c.getExpiresAt().isBefore(Instant.now())) throw ApiException.badRequest("OTP expired. Please request a new one");
        if (c.getAttempts() >= maxAttempts) throw ApiException.badRequest("Too many wrong OTPs. Please start again");
        if (!KycValidator.sha256(code).equals(c.getCodeHash())) {
            c.setAttempts(c.getAttempts() + 1);
            int left = maxAttempts - c.getAttempts();
            throw ApiException.badRequest(left > 0 ? "Incorrect OTP. " + left + " attempt(s) left"
                    : "Incorrect OTP. No attempts left - please start again");
        }
        c.setUsed(true);
        return c;
    }

    public <T> T payload(OtpChallenge c, Class<T> type) {
        try {
            return objectMapper.readValue(c.getPayload(), type);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP request data is invalid - please start again");
        }
    }

    private OtpChallenge load(String id) {
        return repo.findById(parse(id)).orElseThrow(() -> ApiException.badRequest("OTP request not found"));
    }

    private UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (Exception e) {
            throw ApiException.badRequest("OTP request not found");
        }
    }

    private static String newCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String message(String code, String forWhat) {
        return """
                Dear Customer,

                Your One Time Password (OTP) for %s is:

                    %s

                It is valid for %d minutes and can be used only once.
                If you did not request this, someone may be trying to use your account -
                please change your password immediately.
                """.formatted(forWhat, code, validitySeconds / 60);
    }
}
