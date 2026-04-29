package com.oms.agent.tools;

import com.oms.agent.client.ProductClient;
import com.oms.agent.embedding.ProductIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductTools {

    private final ProductClient productClient;
    private final ProductIndexingService productIndexingService;

    @Tool(description = "Search for products by any natural-language query (synonyms, descriptions, categories). " +
          "Returns a list of matching products — each item already includes id, name, category, price, stockQty, imageUrl, and active status. " +
          "DO NOT call getProductDetails for items in these results; the data is complete. " +
          "Omit minPrice and maxPrice entirely if no price filter is needed.")
    public String searchProducts(
            String query,
            @ToolParam(required = false, description = "Minimum price filter. Omit if no lower price bound is needed.") Double minPrice,
            @ToolParam(required = false, description = "Maximum price filter. Omit if no upper price bound is needed.") Double maxPrice) {
        log.info("Tool invoked: searchProducts(query={}, minPrice={}, maxPrice={})", query, minPrice, maxPrice);
        return productIndexingService.semanticSearch(query, minPrice, maxPrice, 15);
    }

    @Tool(description = "Fetch full details for ONE specific product when the user explicitly asks about a single product by name or ID. Do not call this for products already returned by searchProducts.")
    public String getProductDetails(String productId) {
        log.info("Tool invoked: getProductDetails(productId={})", productId);
        return productClient.getProductDetails(productId);
    }
}
