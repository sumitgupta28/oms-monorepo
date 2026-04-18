package com.oms.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.agent.exception.AgentToolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.product-url:http://product-service:8084}")
    private String productUrl;

    public String searchProducts(String query, Double minPrice, Double maxPrice) {
        log.info("searchProducts(graphql): query={} minPrice={} maxPrice={}", query, minPrice, maxPrice);
        try {
            StringBuilder args = new StringBuilder("query: ").append(jsonStr(query));
            if (minPrice != null) args.append(", minPrice: ").append(jsonStr(minPrice.toString()));
            if (maxPrice != null) args.append(", maxPrice: ").append(jsonStr(maxPrice.toString()));

            String gql = """
                { searchProducts(%s) {
                    total query
                    results { id name description category price stockQty imageUrl active createdAt }
                  }
                }""".formatted(args);

            JsonNode data = postGraphQL(gql).path("data").path("searchProducts");
            return objectMapper.writeValueAsString(data);
        } catch (AgentToolException e) {
            throw e;
        } catch (Exception e) {
            log.error("searchProducts(graphql) failed for query '{}': {}", query, e.getMessage(), e);
            throw new AgentToolException("searchProducts", e.getMessage(), e);
        }
    }

    public String getProductDetails(String productId) {
        log.info("getProductDetails(graphql): productId={}", productId);
        try {
            String gql = """
                { product(id: %s) {
                    id name description category price stockQty imageUrl active createdAt
                  }
                }""".formatted(jsonStr(productId));

            JsonNode data = postGraphQL(gql).path("data").path("product");
            if (data.isMissingNode() || data.isNull()) {
                throw new AgentToolException("getProductDetails", "Product not found: " + productId);
            }
            return objectMapper.writeValueAsString(data);
        } catch (AgentToolException e) {
            throw e;
        } catch (Exception e) {
            log.error("getProductDetails(graphql) failed for product {}: {}", productId, e.getMessage(), e);
            throw new AgentToolException("getProductDetails", "Product not found: " + productId, e);
        }
    }

    private JsonNode postGraphQL(String query) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = objectMapper.writeValueAsString(Map.of("query", query));
        ResponseEntity<String> response = restTemplate.exchange(
            productUrl + "/graphql", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private String jsonStr(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
