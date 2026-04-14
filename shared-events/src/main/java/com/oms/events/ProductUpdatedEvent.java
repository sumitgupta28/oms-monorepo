package com.oms.events;

import java.time.Instant;
import java.util.UUID;

// ─── Product events ────────────────────────────────────────────────────────
public record ProductUpdatedEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    String productId,
    String name,
    String description
) implements DomainEvent {
    public ProductUpdatedEvent(String productId, String name, String description) {
        this(UUID.randomUUID(), "PRODUCT_UPDATED", Instant.now(), productId, name, description);
    }
}
