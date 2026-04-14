package com.oms.agent.client;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component @RequiredArgsConstructor @Slf4j
public class ProductClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${services.product-url:http://product-service:8084}") private String productUrl;

    public String searchProducts(String query) {
        try {
            var r = restTemplate.getForObject(productUrl+"/products/search?q="+query, Object.class);
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            return            "{\"error\":\"" + e.getMessage()+ " \"}";
            }
    }

    public String getProductDetails(String productId) {
        try {
            var r = restTemplate.getForObject(productUrl+"/products/"+productId, Object.class);
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            return "{\"error\":\"Product not found: \"}";
        }
    }

    public String checkInventory(String productId) {
        try {
            var r = restTemplate.getForObject(productUrl.replace("8084","8083")+"/inventory/"+productId, Object.class);
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            return "{\"error\":\"Inventory heck failed \"}";
        }
    }
}
