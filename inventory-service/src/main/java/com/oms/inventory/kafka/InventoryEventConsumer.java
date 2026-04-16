package com.oms.inventory.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.service.InventoryService;
import com.oms.inventory.service.InventoryService.ReserveItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class InventoryEventConsumer {
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics="oms.orders.placed", groupId="oms-inventory-group")
    public void onOrderPlaced(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            UUID orderId = UUID.fromString(n.get("orderId").asText());
            List<ReserveItem> items = new ArrayList<>();
            n.get("items").forEach(i -> items.add(
                new ReserveItem(i.get("productId").asText(), i.get("quantity").asInt())));
            inventoryService.reserveStock(orderId, items);
        } catch (Exception e) { log.error("Error processing OrderPlacedEvent: {}", e.getMessage(), e); }
    }

    @KafkaListener(topics="oms.orders.cancelled", groupId="oms-inventory-group")
    public void onOrderCancelled(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            UUID orderId = UUID.fromString(n.get("orderId").asText());
            inventoryService.releaseStockByOrderId(orderId);
            log.info("Inventory released for cancelled order {}", orderId);
        } catch (Exception e) { log.error("Error processing OrderCancelledEvent: {}", e.getMessage(), e); }
    }

    @KafkaListener(topics="oms.payment.failed", groupId="oms-inventory-group")
    public void onPaymentFailed(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            UUID orderId = UUID.fromString(n.get("orderId").asText());
            inventoryService.releaseStockByOrderId(orderId);
            log.info("Inventory released for failed payment, order {}", orderId);
        } catch (Exception e) { log.error("Error processing PaymentFailedEvent: {}", e.getMessage(), e); }
    }
}
