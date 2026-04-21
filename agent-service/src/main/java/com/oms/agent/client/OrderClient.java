package com.oms.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.agent.exception.AgentToolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${services.order-url:http://order-service:8081}")
    private String orderUrl;

    public String placeOrder(String userId, String productId, String productName,
                              int quantity, double unitPrice) {
        log.info("placeOrder: userId={}, productId={}, quantity={}", userId, productId, quantity);
        try {
            Map<String, Object> item = Map.of(
                "productId", productId,
                "productName", productName,
                "quantity", quantity,
                "unitPrice", unitPrice
            );
            Map<String, Object> body = Map.of(
                "userId", userId,
                "items", List.of(item)
            );
            var response = restClient.post()
                    .uri(orderUrl + "/orders")
                    .body(body)
                    .retrieve()
                    .body(Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("placeOrder failed for user {}: {}", userId, e.getMessage(), e);
            throw new AgentToolException("placeOrder", e.getMessage(), e);
        }
    }

    public String trackOrder(String orderId) {
        log.info("trackOrder: orderId={}", orderId);
        try {
            var response = restClient.get()
                    .uri(orderUrl + "/orders/{id}", orderId)
                    .retrieve()
                    .body(Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("trackOrder failed for order {}: {}", orderId, e.getMessage(), e);
            throw new AgentToolException("trackOrder", "Order not found: " + orderId, e);
        }
    }

    public String cancelOrder(String orderId, String reason) {
        log.info("cancelOrder: orderId={}, reason={}", orderId, reason);
        try {
            Map<String, String> body = Map.of("reason", reason);
            restClient.patch()
                    .uri(orderUrl + "/orders/{id}/cancel", orderId)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return "{\"message\":\"Order " + orderId + " cancelled successfully\"}";
        } catch (Exception e) {
            log.error("cancelOrder failed for order {}: {}", orderId, e.getMessage(), e);
            throw new AgentToolException("cancelOrder", "Cannot cancel order: " + e.getMessage(), e);
        }
    }

    public String getMyOrders(String userId) {
        log.info("getMyOrders: userId={}", userId);
        try {
            var response = restClient.get()
                    .uri(orderUrl + "/orders/my")
                    .retrieve()
                    .body(Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("getMyOrders failed for user {}: {}", userId, e.getMessage(), e);
            throw new AgentToolException("getMyOrders", "Failed to fetch orders", e);
        }
    }
}
