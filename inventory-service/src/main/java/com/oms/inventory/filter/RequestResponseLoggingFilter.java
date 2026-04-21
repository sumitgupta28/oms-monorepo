package com.oms.inventory.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LOG_LENGTH = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isActuatorRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var wrappedRequest = new ContentCachingRequestWrapper(request);
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequest(wrappedRequest);
            logResponse(wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String queryString = request.getQueryString();
        String uri = queryString != null
                ? request.getRequestURI() + "?" + queryString
                : request.getRequestURI();
        String body = extractBody(request.getContentAsByteArray());

        log.info(">>> {} {} | ip={} | body={}",
                request.getMethod(), uri,
                request.getRemoteAddr(),
                body.isEmpty() ? "<none>" : body);
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        String body = extractBody(response.getContentAsByteArray());

        log.info("<<< status={} | duration={}ms | body={}",
                response.getStatus(), duration,
                body.isEmpty() ? "<none>" : body);
    }

    private String extractBody(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        return body.length() > MAX_BODY_LOG_LENGTH
                ? body.substring(0, MAX_BODY_LOG_LENGTH) + "...[truncated]"
                : body;
    }

    private boolean isActuatorRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
