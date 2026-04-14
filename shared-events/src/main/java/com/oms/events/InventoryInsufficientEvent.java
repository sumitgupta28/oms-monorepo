package com.oms.events;

import java.time.Instant;
import java.util.UUID;

public record InventoryInsufficientEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    UUID orderId,
    String productId,
    int requested,
    int available
) implements DomainEvent {
    public InventoryInsufficientEvent(UUID orderId, String productId,
                                       int requested, int available) {
        this(UUID.randomUUID(), "INVENTORY_INSUFFICIENT", Instant.now(),
             orderId, productId, requested, available);
    }
}
