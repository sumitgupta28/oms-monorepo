package com.oms.agent.tools;

import com.oms.agent.client.InventoryClient;
import com.oms.agent.client.ProductClient;
import com.oms.agent.embedding.ProductIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductTools {

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final ProductIndexingService productIndexingService;

    @Tool(description = "Search for products using semantic similarity. Understands natural language, " +
          "synonyms, and descriptions — not just exact keyword matches. " +
          "Pass null for price bounds that are not mentioned.")
    public String searchProducts(String query, Double minPrice, Double maxPrice) {
        log.info("Tool invoked: searchProducts(query={}, minPrice={}, maxPrice={})", query, minPrice, maxPrice);
        return productIndexingService.semanticSearch(query, minPrice, maxPrice, 15);
    }

    @Tool(description = "Get detailed information about a specific product by its ID.")
    public String getProductDetails(String productId) {
        log.info("Tool invoked: getProductDetails(productId={})", productId);
        return productClient.getProductDetails(productId);
    }

    @Tool(description = "Check the current stock level for a product.")
    public String checkInventory(String productId) {
        log.info("Tool invoked: checkInventory(productId={})", productId);
        return inventoryClient.validateStock(productId, 1);
    }
}
