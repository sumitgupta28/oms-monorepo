package com.oms.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component @RequiredArgsConstructor @Slf4j
public class OrderClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${services.order-url:http://order-service:8081}") private String orderUrl;

    public String placeOrder(String userId, String productId, String productName,
                              int quantity, double unitPrice) {
        try {
            Map<String,Object> item = Map.of("productId",productId,"productName",productName,
                "quantity",quantity,"unitPrice",unitPrice);
            Map<String,Object> body = Map.of("items",List.of(item));
            var response = restTemplate.postForObject(orderUrl+"/orders", body, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("placeOrder failed: {}", e.getMessage());
            return "{\"error\":\"Failed to place order: " + e.getMessage() + "\"}";
        }
    }

    public String trackOrder(String orderId) {
        try {
            var response = restTemplate.getForObject(orderUrl+"/orders/"+orderId, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) { return "{\"error\":\"Order not found: " + orderId + "\"}"; }
    }

    public String cancelOrder(String orderId, String reason) {
        try {
            Map<String,String> body = Map.of("reason", reason);
            restTemplate.patchForObject(orderUrl+"/orders/"+orderId+"/cancel", body, Object.class);
            return "{\"message\":\"Order " + orderId + " cancelled successfully\"}";
        } catch (Exception e) { return "{\"error\":\"Cannot cancel order: " + e.getMessage() + "\"}"; }
    }

    public String getMyOrders(String userId) {
        try {
            var response = restTemplate.getForObject(orderUrl+"/orders/my", Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) { return "{\"error\":\"Failed to fetch orders\"}"; }
    }
}
