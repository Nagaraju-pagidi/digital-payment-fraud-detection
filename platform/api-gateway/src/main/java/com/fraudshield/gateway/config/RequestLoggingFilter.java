package com.fraudshield.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Logs every request that passes through the gateway with its status and time taken. */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        return chain.filter(exchange).doFinally(signal -> {
            if (path.endsWith("/stream")) return; // live streams stay open - don't log them
            Integer status = exchange.getResponse().getStatusCode() == null
                    ? null : exchange.getResponse().getStatusCode().value();
            log.info("{} {} -> {} ({} ms)", method, path, status, System.currentTimeMillis() - start);
        });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
