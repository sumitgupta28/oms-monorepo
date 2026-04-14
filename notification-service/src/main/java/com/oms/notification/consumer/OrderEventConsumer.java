package com.oms.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.notification.sender.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class OrderEventConsumer {
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics="oms.orders.placed", groupId="oms-notification-group")
    public void onOrderPlaced(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            String email = n.path("userEmail").asText("unknown@oms.com");
            String orderId = n.path("orderId").asText();
            emailSender.send(email, "Order placed: " + orderId,
                "Thank you! Order " + orderId + " is being processed.");
        } catch (Exception e) { log.error("onOrderPlaced error: {}", e.getMessage()); }
    }

    @KafkaListener(topics="oms.orders.shipped", groupId="oms-notification-group")
    public void onOrderShipped(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            String email    = n.path("userEmail").asText("unknown@oms.com");
            String orderId  = n.path("orderId").asText();
            String tracking = n.path("trackingNumber").asText("N/A");
            emailSender.send(email, "Order shipped! Tracking: " + tracking,
                "Order " + orderId + " shipped. Tracking: " + tracking);
        } catch (Exception e) { log.error("onOrderShipped error: {}", e.getMessage()); }
    }

    @KafkaListener(topics="oms.orders.cancelled", groupId="oms-notification-group")
    public void onOrderCancelled(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            String email   = n.path("userEmail").asText("unknown@oms.com");
            String orderId = n.path("orderId").asText();
            emailSender.send(email, "Order cancelled: " + orderId,
                "Your order " + orderId + " has been cancelled.");
        } catch (Exception e) { log.error("onOrderCancelled error: {}", e.getMessage()); }
    }
}
