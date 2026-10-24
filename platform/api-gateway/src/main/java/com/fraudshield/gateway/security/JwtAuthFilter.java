package com.fraudshield.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Security at the front door.
 *
 * 1. Public URLs (login / register) pass through.
 * 2. Every other /api/** call must carry a valid JWT token
 *    ("Authorization: Bearer xxx", or "?access_token=xxx" for live streams).
 * 3. The token's role must match the area:
 *      CUSTOMER -> /api/users, /api/payments         (FraudPay app)
 *      ANALYST  -> /api/fraud, /api/insights,
 *                  /api/notifications, /api/simulator (FraudShield Control Center)
 * 4. The user's id / role / name are forwarded to the services as headers
 *    (X-User-Id, X-User-Role, X-User-Name). Any such headers sent by the
 *    client are removed first, so nobody can fake them.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/",          // FraudPay customer login / register / cities
            "/api/fraud/auth/"     // FraudShield analyst login
    );
    private static final List<String> ANALYST_PREFIXES = List.of(
            "/api/fraud/", "/api/insights/", "/api/notifications/", "/api/simulator/");
    private static final List<String> CUSTOMER_PREFIXES = List.of(
            "/api/users/", "/api/payments/");

    private final SecretKey key;

    public JwtAuthFilter(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Strip identity headers that a client might try to fake
        ServerHttpRequest cleaned = request.mutate().headers(h -> {
            h.remove("X-User-Id");
            h.remove("X-User-Role");
            h.remove("X-User-Name");
        }).build();

        if (!path.startsWith("/api/") || HttpMethod.OPTIONS.equals(request.getMethod()) || isPublic(path)) {
            return chain.filter(exchange.mutate().request(cleaned).build());
        }

        String token = extractToken(request);
        if (token == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Login required");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid token for {}: {}", path, ex.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Session expired - please login again");
        }

        String role = String.valueOf(claims.get("role"));
        String requiredRole = requiredRole(path);
        if (requiredRole != null && !requiredRole.equals(role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "This area needs role " + requiredRole);
        }

        final String userId = claims.getSubject();
        final String name = claims.get("name") == null ? ""
                : String.valueOf(claims.get("name")).replaceAll("[^\\x20-\\x7E]", "");
        ServerHttpRequest authenticated = cleaned.mutate().headers(h -> {
            h.set("X-User-Id", userId);
            h.set("X-User-Role", role);
            h.set("X-User-Name", name);
        }).build();

        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String requiredRole(String path) {
        String p = path.endsWith("/") ? path : path + "/";
        if (ANALYST_PREFIXES.stream().anyMatch(p::startsWith)) return "ANALYST";
        if (CUSTOMER_PREFIXES.stream().anyMatch(p::startsWith)) return "CUSTOMER";
        return null;
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        // Browsers' EventSource (live updates) cannot send headers, so the token comes as a query param
        String queryToken = request.getQueryParams().getFirst("access_token");
        return (queryToken == null || queryToken.isBlank()) ? null : queryToken;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // run before routing
    }
}
