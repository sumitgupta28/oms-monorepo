package com.oms.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// ─── Inventory events ──────────────────────────────────────────────────────
public record InventoryReservedEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    UUID orderId,
    List<String> productIds
) implements DomainEvent {
    public InventoryReservedEvent(UUID orderId, List<String> productIds) {
        this(UUID.randomUUID(), "INVENTORY_RESERVED", Instant.now(), orderId, productIds);
    }
}
