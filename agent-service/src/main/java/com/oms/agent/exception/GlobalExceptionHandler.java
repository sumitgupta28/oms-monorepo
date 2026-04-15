package com.oms.agent.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentToolException.class)
    public ProblemDetail handleAgentToolException(AgentToolException ex) {
        log.error("Tool execution failed: {} - {}", ex.getToolName(), ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        pd.setTitle("Tool Execution Failed");
        pd.setType(URI.create("https://oms.io/errors/tool-failure"));
        pd.setProperty("tool", ex.getToolName());
        return pd;
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleRestClientException(RestClientException ex) {
        log.error("Downstream service call failed: {}", ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
            "Downstream service unavailable: " + ex.getMessage());
        pd.setTitle("Service Unavailable");
        pd.setType(URI.create("https://oms.io/errors/service-unavailable"));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("https://oms.io/errors/internal-error"));
        return pd;
    }
}