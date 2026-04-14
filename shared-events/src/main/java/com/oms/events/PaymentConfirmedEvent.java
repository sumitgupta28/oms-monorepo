package com.oms.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// ─── Payment events ────────────────────────────────────────────────────────
public record PaymentConfirmedEvent(
    UUID correlationId,
    String eventType,
    Instant timestamp,
    UUID paymentId,
    UUID orderId,
    BigDecimal amount
) implements DomainEvent {
    public PaymentConfirmedEvent(UUID paymentId, UUID orderId, BigDecimal amount) {
        this(UUID.randomUUID(), "PAYMENT_CONFIRMED", Instant.now(), paymentId, orderId, amount);
    }
}
