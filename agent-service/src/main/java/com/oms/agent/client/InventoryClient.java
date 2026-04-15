package com.oms.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.agent.exception.AgentToolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.inventory-url:http://inventory-service:8083}")
    private String inventoryUrl;

    public String validateStock(String productId, int qty) {
        log.info("validateStock: productId={}, quantity={}", productId, qty);
        try {
            var response = restTemplate.getForObject(
                inventoryUrl + "/inventory/" + productId, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("validateStock failed for product {}: {}", productId, e.getMessage(), e);
            throw new AgentToolException("validateStock", "Cannot validate stock for product: " + productId, e);
        }
    }
}
