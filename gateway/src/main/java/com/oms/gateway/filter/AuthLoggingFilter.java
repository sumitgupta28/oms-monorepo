package com.oms.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!request.getPath().value().startsWith("/auth")) {
            return chain.filter(exchange);
        }

        String method = request.getMethod().name();
        String path = request.getPath().value();
        String requestId = request.getId();

        log.info("[AUTH] --> {} {} | id={}", method, path, requestId);

        long start = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(signalType -> {
            ServerHttpResponse response = exchange.getResponse();
            int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
            long duration = System.currentTimeMillis() - start;
            log.info("[AUTH] <-- {} {} | status={} duration={}ms id={}", method, path, status, duration, requestId);
        });
    }
}
