package com.fraudpay.payment.web;

import com.fraudpay.payment.dto.*;
import com.fraudpay.payment.geo.GeoCatalog;
import com.fraudpay.payment.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Public endpoints (no session needed): account opening, login, OTP, reference lists. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final GeoCatalog geo;

    public AuthController(AuthService auth, GeoCatalog geo) {
        this.auth = auth;
        this.geo = geo;
    }

    /** Account opening step 1 - validates KYC and emails an OTP. */
    @PostMapping("/register")
    public OtpChallengeResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.startRegistration(request);
    }

    /** Account opening step 2 - OTP -> account created. */
    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResult verifyRegistration(@Valid @RequestBody OtpVerifyRequest request) {
        return auth.completeRegistration(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return auth.login(request, ClientInfo.ip(http));
    }

    /** Only for a new device: the email OTP. */
    @PostMapping("/login/verify")
    public LoginResponse verifyLogin(@Valid @RequestBody OtpVerifyRequest request) {
        return auth.completeLogin(request);
    }

    @PostMapping("/otp/resend")
    public OtpChallengeResponse resend(@RequestBody Map<String, String> body) {
        return auth.resendOtp(body.get("challengeId"));
    }

    /** Lists for the account-opening form. */
    @GetMapping("/reference-data")
    public Map<String, Object> referenceData() {
        return Map.of(
                "cities", geo.homeCities(),
                "genders", AuthService.GENDERS,
                "occupations", AuthService.OCCUPATIONS,
                "incomes", AuthService.INCOMES,
                "relations", AuthService.RELATIONS,
                "accountTypes", List.of("SAVINGS", "CURRENT"));
    }

    /** Turns the browser's GPS position into a place name (shown in the security panel). */
    @GetMapping("/locate")
    public GeoCatalog.GeoMatch locate(@RequestParam double lat, @RequestParam double lon) {
        return geo.nearest(lat, lon);
    }
}
