package com.oms.agent.tools;
import com.oms.agent.client.ProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class ProductTools {
    private final ProductClient productClient;

    @Tool(description = "Search for products using natural language. Use for queries like find me a laptop under 1000 or best gaming headset.")
    public String searchProducts(String query) {
        return productClient.searchProducts(query);
    }

    @Tool(description = "Get detailed information about a specific product by its ID.")
    public String getProductDetails(String productId) {
        return productClient.getProductDetails(productId);
    }

    @Tool(description = "Check the current stock level for a product.")
    public String checkInventory(String productId) {
        return productClient.checkInventory(productId);
    }
}
