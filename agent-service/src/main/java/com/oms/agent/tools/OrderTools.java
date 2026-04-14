package com.oms.agent.tools;
import com.oms.agent.client.OrderClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class OrderTools {
    private final OrderClient orderClient;

    @Tool(description = "Place a new order. Call this when the user wants to purchase products. Requires product details.")
    public String placeOrder(String userId, String productId, String productName,
                              int quantity, double unitPrice) {
        return orderClient.placeOrder(userId, productId, productName, quantity, unitPrice);
    }

    @Tool(description = "Get the current status of an order. Call when user asks to track or check their order.")
    public String trackOrder(String orderId) {
        return orderClient.trackOrder(orderId);
    }

    @Tool(description = "Cancel an order. Only works if the order is in PENDING or VALIDATED state.")
    public String cancelOrder(String orderId, String reason) {
        return orderClient.cancelOrder(orderId, reason);
    }

    @Tool(description = "Get all orders for the current user.")
    public String getMyOrders(String userId) {
        return orderClient.getMyOrders(userId);
    }
}
