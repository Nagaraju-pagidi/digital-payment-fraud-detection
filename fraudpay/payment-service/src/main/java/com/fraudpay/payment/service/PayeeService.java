package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.Beneficiary;
import com.fraudpay.payment.domain.OtpChallenge;
import com.fraudpay.payment.domain.User;
import com.fraudpay.payment.dto.*;
import com.fraudpay.payment.repository.BeneficiaryRepository;
import com.fraudpay.payment.repository.UserRepository;
import com.fraudpay.payment.security.KycValidator;
import com.fraudpay.payment.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Payees (beneficiaries): verify account number + IFSC, confirm with email OTP,
 * then a cooling period before money can be sent (protects against "add payee and drain" fraud).
 */
@Service
public class PayeeService {

    private static final Logger log = LoggerFactory.getLogger(PayeeService.class);

    public record PendingPayee(Long payeeUserId, String name, String nickname, String accountNumber, String ifsc) {
    }

    private final BeneficiaryRepository payees;
    private final UserRepository users;
    private final AccountService accounts;
    private final OtpService otp;
    private final MailService mail;
    private final long coolingMinutes;

    public PayeeService(BeneficiaryRepository payees, UserRepository users, AccountService accounts, OtpService otp,
                        MailService mail, @Value("${fraudpay.payee-cooling-minutes:2}") long coolingMinutes) {
        this.payees = payees;
        this.users = users;
        this.accounts = accounts;
        this.otp = otp;
        this.mail = mail;
        this.coolingMinutes = coolingMinutes;
    }

    @Transactional(readOnly = true)
    public List<PayeeDto> list(Long userId) {
        return payees.findByUserIdAndDeletedFalseOrderByNameAsc(userId).stream()
                .map(b -> Mapper.toDto(b, users.findById(b.getPayeeUserId()).map(User::getBranch).orElse("")))
                .toList();
    }

    /** "Verify account": like the IMPS name check, shows the account holder's name before adding. */
    @Transactional(readOnly = true)
    public Map<String, String> lookup(Long userId, PayeeLookupRequest r) {
        User target = findAccount(userId, r.accountNumber(), r.ifsc());
        return Map.of("name", target.getFullName(), "branch", target.getBranch(), "ifsc", target.getIfsc());
    }

    @Transactional
    public OtpChallengeResponse startAdd(Long userId, AddPayeeRequest r) {
        if (!r.accountNumber().trim().equals(r.confirmAccountNumber().trim())) {
            throw ApiException.badRequest("Account numbers do not match");
        }
        User me = accounts.require(userId);
        User target = findAccount(userId, r.accountNumber(), r.ifsc());
        if (payees.existsByUserIdAndAccountNumberAndDeletedFalse(userId, target.getAccountNumber())) {
            throw ApiException.badRequest("This payee is already added");
        }
        String nickname = r.nickname() == null || r.nickname().isBlank() ? null : r.nickname().trim();
        return otp.start(userId, OtpService.ADD_PAYEE, me.getEmail(),
                "adding payee " + target.getFullName() + " (A/c " + KycValidator.maskAccount(target.getAccountNumber()) + ")",
                new PendingPayee(target.getId(), target.getFullName(), nickname, target.getAccountNumber(), target.getIfsc()));
    }

    @Transactional
    public PayeeDto completeAdd(Long userId, OtpVerifyRequest r) {
        OtpChallenge c = otp.verify(r.challengeId(), OtpService.ADD_PAYEE, userId, r.otp());
        PendingPayee p = otp.payload(c, PendingPayee.class);
        if (payees.existsByUserIdAndAccountNumberAndDeletedFalse(userId, p.accountNumber())) {
            throw ApiException.badRequest("This payee is already added");
        }
        Beneficiary b = payees.save(new Beneficiary(userId, p.payeeUserId(), p.name(), p.nickname(), p.accountNumber(),
                p.ifsc(), Instant.now().plus(Duration.ofMinutes(coolingMinutes))));
        User me = accounts.require(userId);
        mail.notify(me.getEmail(), "New payee added", "Payee " + p.name() + " (A/c "
                + KycValidator.maskAccount(p.accountNumber()) + ") was added to your account. You can transfer money after "
                + coolingMinutes + " minute(s). If you did not add this payee, delete it and contact the bank.");
        log.info("User {} added payee {} (cooling {} min)", userId, p.accountNumber(), coolingMinutes);
        return Mapper.toDto(b, users.findById(p.payeeUserId()).map(User::getBranch).orElse(""));
    }

    @Transactional
    public void delete(Long userId, Long payeeId) {
        Beneficiary b = payees.findByIdAndUserIdAndDeletedFalse(payeeId, userId)
                .orElseThrow(() -> ApiException.notFound("Payee not found"));
        b.setDeleted(true);
    }

    private User findAccount(Long userId, String accountNumber, String ifsc) {
        String acc = accountNumber == null ? "" : accountNumber.trim();
        if (!acc.matches("[0-9]{9,18}")) throw ApiException.badRequest("Account number must be 9-18 digits");
        String code = KycValidator.ifsc(ifsc);
        User target = users.findByAccountNumber(acc)
                .orElseThrow(() -> ApiException.notFound("No account found with this account number"));
        if (!target.getIfsc().equals(code)) {
            throw ApiException.badRequest("Account number and IFSC do not match (the IFSC of this account is different)");
        }
        if (target.getId().equals(userId)) throw ApiException.badRequest("You cannot add your own account as a payee");
        return target;
    }
}
