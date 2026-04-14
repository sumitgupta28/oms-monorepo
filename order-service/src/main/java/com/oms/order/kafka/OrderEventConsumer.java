package com.oms.order.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService  orderService;
    private final ObjectMapper  objectMapper;

    @KafkaListener(topics = "oms.payment.confirmed", groupId = "oms-order-group")
    public void onPaymentConfirmed(String payload) {
        try {
            JsonNode node    = objectMapper.readTree(payload);
            UUID     orderId = UUID.fromString(node.get("orderId").asText());
            orderService.markPaid(orderId);
            log.info("Payment confirmed received for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to process PaymentConfirmedEvent: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "oms.inventory.reserved", groupId = "oms-order-group")
    public void onInventoryReserved(String payload) {
        try {
            JsonNode node    = objectMapper.readTree(payload);
            UUID     orderId = UUID.fromString(node.get("orderId").asText());
            orderService.markValidated(orderId);
            log.info("Inventory reserved received for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to process InventoryReservedEvent: {}", e.getMessage());
        }
    }
}
