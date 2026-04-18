package com.oms.agent.tools;

import com.oms.agent.client.InventoryClient;
import com.oms.agent.client.ProductClient;
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

    @Tool(description = "Search for products by keyword with optional price range. " +
          "Extract minPrice and maxPrice from phrases like 'under $1000', 'between $200 and $500', 'over $50'. " +
          "Pass null for price bounds that are not mentioned.")
    public String searchProducts(String query, Double minPrice, Double maxPrice) {
        log.info("Tool invoked: searchProducts(query={}, minPrice={}, maxPrice={})", query, minPrice, maxPrice);
        return productClient.searchProducts(query, minPrice, maxPrice);
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
