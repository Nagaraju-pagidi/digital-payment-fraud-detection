package com.fraudshield.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway - the single front door of the whole system (port 8060).
 * - Routes every /api/** request to the right microservice (found through Eureka)
 * - Checks the JWT login token and the user's role before forwarding
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
