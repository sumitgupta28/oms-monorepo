package com.oms.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
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
public class ProductClient {

    private final RestClient restClient;
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

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllProducts(int page, int size) {
        log.info("getAllProducts(graphql): page={} size={}", page, size);
        try {
            String gql = """
                { products(page: %d, size: %d) {
                    content { id name description category price stockQty imageUrl active }
                    totalPages
                  }
                }""".formatted(page, size);

            JsonNode content = postGraphQL(gql).path("data").path("products").path("content");
            if (content.isMissingNode() || content.isNull()) return List.of();
            return objectMapper.readValue(content.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            log.warn("getAllProducts(graphql) failed page={}: {}", page, e.getMessage());
            return List.of();
        }
    }

    private JsonNode postGraphQL(String query) throws Exception {
        String responseBody = restClient.post()
                .uri(productUrl + "/graphql")
                .body(Map.of("query", query))
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(responseBody);
    }

    private String jsonStr(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
