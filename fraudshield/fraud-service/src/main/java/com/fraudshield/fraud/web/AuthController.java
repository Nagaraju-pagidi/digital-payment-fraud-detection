package com.fraudshield.fraud.web;

import com.fraudshield.fraud.dto.LoginRequest;
import com.fraudshield.fraud.dto.LoginResponse;
import com.fraudshield.fraud.service.AnalystAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud/auth")
public class AuthController {

    private final AnalystAuthService auth;

    public AuthController(AnalystAuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }
}
