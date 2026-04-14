package com.oms.events;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    UUID orderId,
    String userId,
    String reason
) implements DomainEvent {
    public OrderCancelledEvent(UUID orderId, String userId, String reason) {
        this(UUID.randomUUID(), "ORDER_CANCELLED", Instant.now(), orderId, userId, reason);
    }
}
