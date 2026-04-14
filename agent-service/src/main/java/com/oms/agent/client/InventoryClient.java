package com.oms.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class InventoryClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${services.inventory-url:http://inventory-service:8083}")
    private String inventoryUrl;

    public String validateStock(String productId, int qty) {
        try {
            var r = restTemplate.getForObject(inventoryUrl + "/inventory/" + productId, Object.class);
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            return
                    """
                            {"error":"Cannot validate stock"}
                            """;
        }
    }
}
