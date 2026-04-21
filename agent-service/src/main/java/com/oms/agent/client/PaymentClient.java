package com.oms.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.agent.exception.AgentToolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${services.payment-url:http://payment-service:8082}")
    private String paymentUrl;

    public String initiatePayment(String orderId, double amount) {
        log.info("initiatePayment: orderId={}, amount={}", orderId, amount);
        try {
            Map<String, Object> body = Map.of("orderId", orderId, "amount", amount);
            var response = restClient.post()
                    .uri(paymentUrl + "/payments")
                    .header("Idempotency-Key", "pay-" + orderId)
                    .body(body)
                    .retrieve()
                    .body(Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("initiatePayment failed for order {}: {}", orderId, e.getMessage(), e);
            throw new AgentToolException("initiatePayment", "Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    public String getPaymentStatus(String paymentId) {
        log.info("getPaymentStatus: paymentId={}", paymentId);
        try {
            var response = restClient.get()
                    .uri(paymentUrl + "/payments/{id}", paymentId)
                    .retrieve()
                    .body(Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("getPaymentStatus failed for payment {}: {}", paymentId, e.getMessage(), e);
            throw new AgentToolException("getPaymentStatus", "Payment not found: " + paymentId, e);
        }
    }
}
