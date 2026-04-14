package com.oms.events;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    UUID orderId,
    String userId,
    String trackingNumber
) implements DomainEvent {
    public OrderShippedEvent(UUID orderId, String userId, String trackingNumber) {
        this(UUID.randomUUID(), "ORDER_SHIPPED", Instant.now(), orderId, userId, trackingNumber);
    }
}
