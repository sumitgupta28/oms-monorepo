package com.oms.agent.tools;
import com.oms.agent.client.InventoryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class ValidationTools {
    private final InventoryClient inventoryClient;

    @Tool(description = "Validate whether sufficient stock is available before placing an order.")
    public String validateStock(String productId, int requestedQty) {
        return inventoryClient.validateStock(productId, requestedQty);
    }
}
