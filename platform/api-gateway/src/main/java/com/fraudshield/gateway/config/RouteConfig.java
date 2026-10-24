package com.fraudshield.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Which URL goes to which microservice.
 * "lb://name" means: ask Eureka where "name" is running and load-balance across instances.
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // ---------- FraudPay (payment system) ----------
                .route("payment-service", r -> r
                        .path("/api/auth/**", "/api/users/**", "/api/payments/**", "/api/simulator/**")
                        .uri("lb://payment-service"))
                // ---------- FraudShield (fraud platform) ----------
                .route("fraud-service", r -> r
                        .path("/api/fraud/**", "/api/insights/**")
                        .uri("lb://fraud-service"))
                .build();
    }
}
