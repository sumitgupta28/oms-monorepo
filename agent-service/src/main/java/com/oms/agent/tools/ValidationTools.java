package com.oms.agent.tools;
import com.oms.agent.client.InventoryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class ValidationTools {
    private final InventoryClient inventoryClient;

    @Tool(description = "Check whether a product has enough stock before placing an order. " +
          "Always call this before placeOrder. Returns available quantity and whether the request can be fulfilled.")
    public String validateStock(
            @ToolParam(description = "Product ID from searchProducts results") String productId,
            @ToolParam(description = "Quantity the user wants to order") int requestedQty) {
        return inventoryClient.validateStock(productId, requestedQty);
    }
}
