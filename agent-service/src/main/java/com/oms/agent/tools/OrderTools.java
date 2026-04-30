package com.oms.agent.tools;
import com.oms.agent.client.OrderClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class OrderTools {
    private final OrderClient orderClient;

    @Tool(description = "Place a new order for the current user. " +
          "Only call AFTER validateStock confirms availability. " +
          "Use the userId from the user context — do NOT ask the user for it.")
    public String placeOrder(
            @ToolParam(description = "The current user's ID (from user context)") String userId,
            @ToolParam(description = "Product ID from searchProducts results") String productId,
            @ToolParam(description = "Product name from searchProducts results") String productName,
            @ToolParam(description = "Quantity to order (must be > 0)") int quantity,
            @ToolParam(description = "Unit price from searchProducts results") double unitPrice,
            ToolContext toolContext) {
        ToolContextHelper.emitToolCall(toolContext, "placeOrder");
        return orderClient.placeOrder(userId, productId, productName, quantity, unitPrice);
    }

    @Tool(description = "Get the current status and details of a specific order by its order ID.")
    public String trackOrder(
            @ToolParam(description = "The order ID returned by placeOrder") String orderId,
            ToolContext toolContext) {
        ToolContextHelper.emitToolCall(toolContext, "trackOrder");
        return orderClient.trackOrder(orderId);
    }

    @Tool(description = "Cancel an order. Only succeeds if the order is in PENDING or VALIDATED state. " +
          "Always confirm with the user before cancelling.")
    public String cancelOrder(
            @ToolParam(description = "The order ID to cancel") String orderId,
            @ToolParam(description = "Reason for cancellation") String reason,
            ToolContext toolContext) {
        ToolContextHelper.emitToolCall(toolContext, "cancelOrder");
        return orderClient.cancelOrder(orderId, reason);
    }

    @Tool(description = "Get all orders placed by the current user.")
    public String getMyOrders(
            @ToolParam(description = "The current user's ID (from user context)") String userId,
            ToolContext toolContext) {
        ToolContextHelper.emitToolCall(toolContext, "getMyOrders");
        return orderClient.getMyOrders(userId);
    }
}
