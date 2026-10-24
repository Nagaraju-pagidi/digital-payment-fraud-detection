package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.Beneficiary;
import com.fraudpay.payment.domain.Payment;
import com.fraudpay.payment.domain.User;
import com.fraudpay.payment.dto.PayeeDto;
import com.fraudpay.payment.dto.PaymentDto;
import com.fraudpay.payment.dto.ProfileDto;
import com.fraudpay.payment.dto.UserDto;
import com.fraudpay.payment.security.KycValidator;

import java.util.Arrays;
import java.util.List;

public final class Mapper {

    private Mapper() {
    }

    public static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getCustomerId(), u.getUsername(), u.getFullName(),
                KycValidator.maskEmail(u.getEmail()), "XXXXXX" + u.getPhone().substring(6),
                u.getAccountNumber(), u.getIfsc(), u.getBranch(), u.getAccountType().name(), u.getUpiId(),
                u.getBalance(), u.getStatus().name(), u.getStatusReason(), u.getHomeCity(),
                u.getLastLoginAt(), u.getCreatedAt());
    }

    public static ProfileDto toProfile(User u) {
        String pan = u.getPan();
        return new ProfileDto(u.getCustomerId(), u.getUsername(), u.getFullName(), u.getDateOfBirth(), u.getGender(),
                KycValidator.maskEmail(u.getEmail()), "XXXXXX" + u.getPhone().substring(6),
                pan.substring(0, 5) + "****" + pan.substring(9), "XXXX XXXX " + u.getAadhaarLast4(),
                u.getAddressLine(), u.getCity(), u.getState(), u.getPincode(), u.getOccupation(), u.getAnnualIncome(),
                u.getNomineeName(), u.getNomineeRelation(), u.getAccountType().name(), u.getAccountNumber(),
                u.getIfsc(), u.getBranch(), u.getUpiId(), u.getHomeCity(), u.getCreatedAt());
    }

    public static PayeeDto toDto(Beneficiary b, String branch) {
        return new PayeeDto(b.getId(), b.getName(), b.getNickname(), b.getAccountNumber(), b.getIfsc(), branch,
                b.isActive(), b.getActiveFrom(), b.getCreatedAt());
    }

    /** The same payment looks different to the sender (SENT) and the receiver (RECEIVED). */
    public static PaymentDto toDto(Payment p, Long viewerId) {
        boolean sent = p.getSenderId().equals(viewerId);
        List<String> reasons = (p.getFraudReasons() == null || p.getFraudReasons().isBlank())
                ? List.of()
                : Arrays.stream(p.getFraudReasons().split(" \\| ")).toList();
        return new PaymentDto(
                p.getId(),
                p.getReferenceNo(),
                p.getChannel(),
                sent ? "SENT" : "RECEIVED",
                sent ? p.getReceiverName() : p.getSenderName(),
                sent ? p.getReceiverUpi() : p.getSenderUpi(),
                sent ? KycValidator.maskAccount(p.getReceiverAccount()) : null,
                p.getAmount(),
                p.getNote(),
                p.getStatus().name(),
                p.getStatusMessage(),
                sent ? p.getFraudScore() : null,
                sent ? reasons : List.of(),
                sent ? p.getLocationCity() : null,
                sent ? p.getLocationCountry() : null,
                sent ? p.getDeviceLabel() : null,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
