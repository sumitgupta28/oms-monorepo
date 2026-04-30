package com.oms.agent.tools;
import com.oms.agent.client.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class PaymentTools {
    private final PaymentClient paymentClient;

    @Tool(description = "Initiate payment for a placed order. Call this after placeOrder succeeds. " +
          "The amount must equal quantity × unitPrice from the order.")
    public String initiatePayment(
            @ToolParam(description = "Order ID returned by placeOrder") String orderId,
            @ToolParam(description = "Payment amount in USD, must equal quantity × unitPrice") double amount,
            ToolContext toolContext) {
        ToolContextHelper.emitToolCall(toolContext, "initiatePayment");
        return paymentClient.initiatePayment(orderId, amount);
    }

    @Tool(description = "Check the current status of a payment. Returns PENDING, CONFIRMED, or FAILED.")
    public String checkPaymentStatus(
            @ToolParam(description = "Payment ID returned by initiatePayment") String paymentId,
            ToolContext toolContext) {
        ToolContextHelper.emitToolCall(toolContext, "checkPaymentStatus");
        return paymentClient.getPaymentStatus(paymentId);
    }
}
