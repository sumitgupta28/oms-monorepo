package com.oms.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// ─── Base event ────────────────────────────────────────────────────────────
public sealed interface DomainEvent permits
    OrderPlacedEvent, OrderCancelledEvent, OrderShippedEvent,
    PaymentConfirmedEvent, PaymentFailedEvent,
    InventoryReservedEvent, InventoryInsufficientEvent,
    ProductUpdatedEvent {

    UUID   correlationId();
    String eventType();
    Instant timestamp();
}

