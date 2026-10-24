package com.fraudpay.payment.security;

import com.fraudpay.payment.web.ApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Period;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Bank-style validation rules used at account opening and in security settings.
 * Every method throws ApiException(400) with a clear message when the value is not valid.
 */
public final class KycValidator {

    private static final Pattern PAN = Pattern.compile("[A-Z]{3}P[A-Z][0-9]{4}[A-Z]");
    private static final Pattern AADHAAR = Pattern.compile("[2-9][0-9]{11}");
    private static final Pattern MOBILE = Pattern.compile("[6-9][0-9]{9}");
    private static final Pattern PINCODE = Pattern.compile("[1-9][0-9]{5}");
    private static final Pattern IFSC = Pattern.compile("[A-Z]{4}0[A-Z0-9]{6}");
    private static final Pattern USERNAME = Pattern.compile("[a-z][a-z0-9._]{4,19}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .']{1,98}[A-Za-z]");

    // Verhoeff checksum tables (the algorithm UIDAI uses for the last digit of Aadhaar)
    private static final int[][] D = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, {1, 2, 3, 4, 0, 6, 7, 8, 9, 5}, {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7}, {4, 0, 1, 2, 3, 9, 5, 6, 7, 8}, {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2}, {7, 6, 5, 9, 8, 2, 1, 0, 4, 3}, {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}};
    private static final int[][] P = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, {1, 5, 7, 6, 2, 8, 3, 0, 9, 4}, {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7}, {9, 4, 5, 3, 1, 2, 6, 8, 7, 0}, {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5}, {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}};

    private KycValidator() {
    }

    public static String name(String value, String label) {
        String v = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (!NAME.matcher(v).matches()) {
            throw ApiException.badRequest(label + " should contain only letters and spaces (3-100 characters)");
        }
        return v;
    }

    public static void adult(LocalDate dob) {
        if (dob == null) throw ApiException.badRequest("Date of birth is required");
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (dob.isAfter(LocalDate.now())) throw ApiException.badRequest("Date of birth cannot be in the future");
        if (age < 18) throw ApiException.badRequest("You must be at least 18 years old to open an account");
        if (age > 100) throw ApiException.badRequest("Please check the date of birth");
    }

    public static String mobile(String value) {
        String v = value == null ? "" : value.trim();
        if (!MOBILE.matcher(v).matches()) {
            throw ApiException.badRequest("Mobile number must be 10 digits and start with 6, 7, 8 or 9");
        }
        return v;
    }

    /**
     * PAN of an individual: AAAPL1234C - 5 letters, 4 digits, 1 letter.
     * 4th letter must be 'P' (person); 5th letter = first letter of the surname.
     */
    public static String pan(String value, String fullName) {
        String v = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!PAN.matcher(v).matches()) {
            throw ApiException.badRequest("Invalid PAN. Format is ABCPD1234E (4th letter must be P for individuals)");
        }
        String[] parts = fullName.trim().split("\\s+");
        char surnameInitial = Character.toUpperCase(parts[parts.length - 1].charAt(0));
        if (v.charAt(4) != surnameInitial) {
            throw ApiException.badRequest("PAN does not match the name: 5th letter of PAN must be '"
                    + surnameInitial + "' (first letter of your surname " + parts[parts.length - 1] + ")");
        }
        return v;
    }

    /** Aadhaar: 12 digits, cannot start with 0 or 1, last digit is a Verhoeff checksum. */
    public static String aadhaar(String value) {
        String v = value == null ? "" : value.replaceAll("\\s", "");
        if (!AADHAAR.matcher(v).matches()) {
            throw ApiException.badRequest("Aadhaar must be 12 digits and cannot start with 0 or 1");
        }
        int c = 0;
        for (int i = 0; i < v.length(); i++) {
            int digit = v.charAt(v.length() - 1 - i) - '0';
            c = D[c][P[i % 8][digit]];
        }
        if (c != 0) {
            throw ApiException.badRequest("Invalid Aadhaar number (checksum does not match). Please check the digits");
        }
        return v;
    }

    public static String pincode(String value) {
        String v = value == null ? "" : value.trim();
        if (!PINCODE.matcher(v).matches()) throw ApiException.badRequest("PIN code must be 6 digits");
        return v;
    }

    public static String ifsc(String value) {
        String v = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!IFSC.matcher(v).matches()) {
            throw ApiException.badRequest("Invalid IFSC. Format is 4 letters + 0 + 6 letters/digits (e.g. FRPY0HYD001)");
        }
        return v;
    }

    public static String username(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(v).matches()) {
            throw ApiException.badRequest("User ID must be 5-20 characters: start with a letter; letters, digits, . or _");
        }
        return v;
    }

    /** 8-20 chars with upper, lower, digit and special character, not containing the user id. */
    public static void strongPassword(String password, String username) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            throw ApiException.badRequest("Password must be 8-20 characters long");
        }
        if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*") || !password.matches(".*[^A-Za-z0-9].*")) {
            throw ApiException.badRequest("Password needs an uppercase letter, a lowercase letter, a digit and a special character");
        }
        if (username != null && password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Password must not contain your User ID");
        }
    }

    /** 6-digit transaction PIN that is not easy to guess. */
    public static void strongPin(String pin, LocalDate dob, String mobile) {
        if (pin == null || !pin.matches("[0-9]{6}")) {
            throw ApiException.badRequest("Transaction PIN must be exactly 6 digits");
        }
        if (pin.chars().distinct().count() == 1) {
            throw ApiException.badRequest("Transaction PIN cannot be the same digit repeated");
        }
        if ("0123456789".contains(pin) || "9876543210".contains(pin)) {
            throw ApiException.badRequest("Transaction PIN cannot be a sequence like 123456");
        }
        if (dob != null) {
            String ddmmyy = String.format("%02d%02d%02d", dob.getDayOfMonth(), dob.getMonthValue(), dob.getYear() % 100);
            String yyyymm = String.format("%04d%02d", dob.getYear(), dob.getMonthValue());
            if (pin.equals(ddmmyy) || pin.equals(yyyymm)) {
                throw ApiException.badRequest("Transaction PIN cannot be your date of birth");
            }
        }
        if (mobile != null && (mobile.startsWith(pin) || mobile.endsWith(pin))) {
            throw ApiException.badRequest("Transaction PIN cannot be part of your mobile number");
        }
    }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "*".repeat(Math.max(1, at - 2)) + email.charAt(at - 1) + email.substring(at);
    }

    public static String maskAccount(String account) {
        return account == null || account.length() < 4 ? account : "XXXX" + account.substring(account.length() - 4);
    }
}
