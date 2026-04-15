package com.oms.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.agent.exception.AgentToolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.payment-url:http://payment-service:8082}")
    private String paymentUrl;

    public String initiatePayment(String orderId, double amount) {
        log.info("initiatePayment: orderId={}, amount={}", orderId, amount);
        try {
            Map<String, Object> body = Map.of("orderId", orderId, "amount", amount);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Idempotency-Key", "pay-" + orderId);
            headers.setContentType(MediaType.APPLICATION_JSON);
            var entity = new HttpEntity<>(body, headers);
            var response = restTemplate.postForObject(paymentUrl + "/payments", entity, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("initiatePayment failed for order {}: {}", orderId, e.getMessage(), e);
            throw new AgentToolException("initiatePayment", "Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    public String getPaymentStatus(String paymentId) {
        log.info("getPaymentStatus: paymentId={}", paymentId);
        try {
            var response = restTemplate.getForObject(paymentUrl + "/payments/" + paymentId, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("getPaymentStatus failed for payment {}: {}", paymentId, e.getMessage(), e);
            throw new AgentToolException("getPaymentStatus", "Payment not found: " + paymentId, e);
        }
    }
}
