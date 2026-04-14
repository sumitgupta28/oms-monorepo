package com.oms.agent.tools;
import com.oms.agent.client.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class PaymentTools {
    private final PaymentClient paymentClient;

    @Tool(description = "Initiate payment for an order. Call after order is placed to process payment.")
    public String initiatePayment(String orderId, double amount) {
        return paymentClient.initiatePayment(orderId, amount);
    }

    @Tool(description = "Check the status of a payment by payment ID.")
    public String checkPaymentStatus(String paymentId) {
        return paymentClient.getPaymentStatus(paymentId);
    }
}
