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
public class ProductClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.product-url:http://product-service:8084}")
    private String productUrl;

    public String searchProducts(String query) {
        log.info("searchProducts: query={}", query);
        try {
            var response = restTemplate.getForObject(
                productUrl + "/products/search?q=" + query, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("searchProducts failed for query '{}': {}", query, e.getMessage(), e);
            throw new AgentToolException("searchProducts", e.getMessage(), e);
        }
    }

    public String getProductDetails(String productId) {
        log.info("getProductDetails: productId={}", productId);
        try {
            var response = restTemplate.getForObject(
                productUrl + "/products/" + productId, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("getProductDetails failed for product {}: {}", productId, e.getMessage(), e);
            throw new AgentToolException("getProductDetails", "Product not found: " + productId, e);
        }
    }

    public String checkInventory(String productId) {
        log.info("checkInventory: productId={}", productId);
        try {
            // Note: Inventory is handled by inventory-service (port 8083), not product-service
            // This should be refactored to call inventory-service directly
            var response = restTemplate.getForObject(
                productUrl.replace("8084", "8083") + "/inventory/" + productId, Object.class);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("checkInventory failed for product {}: {}", productId, e.getMessage(), e);
            throw new AgentToolException("checkInventory", "Inventory check failed", e);
        }
    }
}
