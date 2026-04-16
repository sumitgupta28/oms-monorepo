package com.oms.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.notification.sender.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailSender  emailSender;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "oms.orders.placed", groupId = "oms-notification-group")
    public void onOrderPlaced(String payload) { handle(payload, "ORDER_PLACED"); }

    @KafkaListener(topics = "oms.payment.confirmed", groupId = "oms-notification-group")
    public void onPaymentConfirmed(String payload) { handle(payload, "PAYMENT_CONFIRMED"); }

    @KafkaListener(topics = "oms.orders.shipped", groupId = "oms-notification-group")
    public void onOrderShipped(String payload) { handle(payload, "ORDER_SHIPPED"); }

    @KafkaListener(topics = "oms.orders.cancelled", groupId = "oms-notification-group")
    public void onOrderCancelled(String payload) { handle(payload, "ORDER_CANCELLED"); }

    @KafkaListener(topics = "oms.payment.failed", groupId = "oms-notification-group")
    public void onPaymentFailed(String payload) { handle(payload, "PAYMENT_FAILED"); }

    private void handle(String payload, String eventType) {
        try {
            JsonNode node  = objectMapper.readTree(payload);
            String orderId = node.has("orderId") ? node.get("orderId").asText() : "N/A";
            String to      = node.has("userEmail") ? node.get("userEmail").asText() : "customer@oms.com";
            String subject = switch (eventType) {
                case "ORDER_PLACED"      -> "Order Confirmation - " + orderId;
                case "PAYMENT_CONFIRMED" -> "Payment Confirmed - " + orderId;
                case "ORDER_SHIPPED"     -> "Your Order Has Shipped - " + orderId;
                case "ORDER_CANCELLED"   -> "Order Cancelled - " + orderId;
                case "PAYMENT_FAILED"    -> "Payment Failed - " + orderId;
                default                  -> "OMS Notification";
            };
            String body = buildBody(eventType, orderId, node);
            emailSender.send(to, subject, body);
        } catch (Exception e) {
            log.error("Failed to handle {} event: {}", eventType, e.getMessage());
        }
    }

    private String buildBody(String type, String orderId, JsonNode n) {
        return switch (type) {
            case "ORDER_PLACED"      -> "Thank you for your order " + orderId + "!\nTotal: " + n.path("totalAmount").asText("N/A");
            case "PAYMENT_CONFIRMED" -> "Payment confirmed for order " + orderId + ". Your order is being prepared.";
            case "ORDER_SHIPPED"     -> "Order " + orderId + " has shipped! Tracking: " + n.path("trackingNumber").asText("N/A");
            case "ORDER_CANCELLED"   -> "Order " + orderId + " was cancelled. Reason: " + n.path("reason").asText("N/A");
            case "PAYMENT_FAILED"    -> "Payment for order " + orderId + " could not be processed. Reason: " + n.path("reason").asText("N/A") + ". Please contact support.";
            default                  -> "Notification for order " + orderId;
        };
    }
}
