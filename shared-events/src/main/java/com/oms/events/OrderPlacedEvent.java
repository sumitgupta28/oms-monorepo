package com.oms.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// ─── Order events ──────────────────────────────────────────────────────────
public record OrderPlacedEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    UUID orderId,
    String userId,
    List<OrderItem> items,
    BigDecimal totalAmount
) implements DomainEvent {
    public OrderPlacedEvent(UUID orderId, String userId,
                            List<OrderItem> items, BigDecimal totalAmount) {
        this(UUID.randomUUID(), "ORDER_PLACED", Instant.now(),
             orderId, userId, items, totalAmount);
    }

    public record OrderItem(String productId, int quantity, BigDecimal unitPrice) {}
}
