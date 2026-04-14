package com.oms.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component @RequiredArgsConstructor @Slf4j
public class PaymentClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${services.payment-url:http://payment-service:8082}") private String paymentUrl;

    public String initiatePayment(String orderId, double amount) {
        try {
            Map<String,Object> body = Map.of("orderId",orderId,"amount",amount);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Idempotency-Key", "pay-" + orderId);
            headers.setContentType(MediaType.APPLICATION_JSON);
            var entity = new HttpEntity<>(body, headers);
            var response = restTemplate.postForObject(paymentUrl+"/payments", entity, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\":\"Failed to place order: " + e.getMessage() + "\"}";
        }
    }

    public String getPaymentStatus(String paymentId) {
        try {
            var response = restTemplate.getForObject(paymentUrl+"/payments/"+paymentId, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\":\"Payment not found: \"}";
        }
    }
}
