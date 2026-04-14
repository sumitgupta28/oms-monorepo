package com.oms.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    UUID orderId,
    String reason
) implements DomainEvent {
    public PaymentFailedEvent(UUID orderId, String reason) {
        this(UUID.randomUUID(), "PAYMENT_FAILED", Instant.now(), orderId, reason);
    }
}
