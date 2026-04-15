package com.oms.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.domain.Inventory;
import com.oms.inventory.domain.StockMovement;
import com.oms.inventory.exception.InventoryException;
import com.oms.inventory.repository.InventoryRepository;
import com.oms.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class InventoryService {

    private static final String TOPIC_RESERVED    = "oms.inventory.reserved";
    private static final String TOPIC_INSUFFICIENT = "oms.inventory.insufficient";
    private static final String TOPIC_LOW_STOCK   = "oms.inventory.low-stock";

    private final InventoryRepository     inventoryRepo;
    private final StockMovementRepository movementRepo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    @Value("${inventory.low-stock-threshold:5}") private int lowStockThreshold;

    @Transactional
    public void reserveStock(UUID orderId, List<ReserveItem> items) {
        List<String> reserved = new ArrayList<>();
        for (ReserveItem item : items) {
            Inventory inv = inventoryRepo.findByIdWithLock(item.productId())
                .orElseThrow(() -> new InventoryException("Product not found: " + item.productId()));
            if (!inv.canReserve(item.quantity())) {
                publishEvent(TOPIC_INSUFFICIENT, Map.of(
                    "eventType",  "INVENTORY_INSUFFICIENT",
                    "orderId",    orderId.toString(),
                    "productId",  item.productId(),
                    "requested",  String.valueOf(item.quantity()),
                    "available",  String.valueOf(inv.getAvailableQty())
                ));
                log.warn("Insufficient stock for order {} product {}", orderId, item.productId());
                return;
            }
            inv.reserve(item.quantity());
            inventoryRepo.save(inv);
            movementRepo.save(StockMovement.builder()
                .productId(item.productId())
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-item.quantity())
                .orderId(orderId)
                .build());
            reserved.add(item.productId());
            if (inv.getAvailableQty() <= lowStockThreshold) {
                publishEvent(TOPIC_LOW_STOCK, Map.of(
                    "productId",    item.productId(),
                    "availableQty", String.valueOf(inv.getAvailableQty())
                ));
            }
        }
        publishEvent(TOPIC_RESERVED, Map.of(
            "eventType",  "INVENTORY_RESERVED",
            "orderId",    orderId.toString(),
            "productIds", String.join(",", reserved)
        ));
        log.info("Inventory reserved for order {}", orderId);
    }

    @Transactional
    public void releaseStock(UUID orderId, List<ReserveItem> items) {
        for (ReserveItem item : items) {
            inventoryRepo.findByIdWithLock(item.productId()).ifPresent(inv -> {
                inv.release(item.quantity());
                inventoryRepo.save(inv);
                movementRepo.save(StockMovement.builder()
                    .productId(item.productId())
                    .movementType(StockMovement.MovementType.RELEASE)
                    .delta(item.quantity())
                    .orderId(orderId)
                    .build());
            });
        }
        log.info("Inventory released for order {}", orderId);
    }

    @Transactional
    public void releaseStockByOrderId(UUID orderId) {
        List<StockMovement> reservations = movementRepo.findByOrderIdAndMovementType(
            orderId, StockMovement.MovementType.RESERVE);
        for (StockMovement reservation : reservations) {
            int qty = Math.abs(reservation.getDelta());
            inventoryRepo.findByIdWithLock(reservation.getProductId()).ifPresent(inv -> {
                inv.release(qty);
                inventoryRepo.save(inv);
                movementRepo.save(StockMovement.builder()
                    .productId(reservation.getProductId())
                    .movementType(StockMovement.MovementType.RELEASE)
                    .delta(qty)
                    .orderId(orderId)
                    .build());
            });
        }
        log.info("Inventory released for cancelled order {}", orderId);
    }

    @Transactional
    public InventoryResponse setStock(String productId, int qty) {
        Inventory inv = inventoryRepo.findById(productId)
            .orElse(Inventory.builder().productId(productId).build());
        inv.setAvailableQty(qty);
        return InventoryResponse.from(inventoryRepo.save(inv));
    }

    @Transactional
    public InventoryResponse adjustStock(String productId, int delta) {
        Inventory inv = inventoryRepo.findByIdWithLock(productId)
            .orElseThrow(() -> new InventoryException("Product not found: " + productId));
        inv.addStock(delta);
        movementRepo.save(StockMovement.builder()
            .productId(productId)
            .movementType(StockMovement.MovementType.RESTOCK)
            .delta(delta)
            .build());
        return InventoryResponse.from(inventoryRepo.save(inv));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getStock(String productId) {
        return InventoryResponse.from(inventoryRepo.findById(productId)
            .orElse(Inventory.builder().productId(productId).build()));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllStock() {
        return inventoryRepo.findAll().stream().map(InventoryResponse::from).toList();
    }

    private void publishEvent(String topic, Map<String, String> payload) {
        try {
            kafka.send(topic, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to publish event to {}: {}", topic, e.getMessage(), e);
        }
    }

    public record ReserveItem(String productId, int quantity) {}
}
