package com.fraudshield.fraud.service;

import com.fraudshield.fraud.domain.Analyst;
import com.fraudshield.fraud.dto.LoginRequest;
import com.fraudshield.fraud.dto.LoginResponse;
import com.fraudshield.fraud.repository.AnalystRepository;
import com.fraudshield.fraud.security.JwtService;
import com.fraudshield.fraud.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Analyst login. A demo analyst (analyst / password123) is created on first start. */
@Service
public class AnalystAuthService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalystAuthService.class);

    private final AnalystRepository analysts;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AnalystAuthService(AnalystRepository analysts, PasswordEncoder encoder, JwtService jwt) {
        this.analysts = analysts;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest req) {
        Analyst a = analysts.findByUsernameIgnoreCase(req.username().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong username or password"));
        return new LoginResponse(jwt.issueToken(a), a.getUsername(), a.getFullName());
    }

    @Override
    public void run(String... args) {
        if (analysts.count() == 0) {
            analysts.save(new Analyst("analyst", "Anita Rao", encoder.encode("password123")));
            log.info("Seeded demo analyst: analyst / password123");
        }
    }
}
