package com.oms.order.domain;

public enum OrderStatus {
    PENDING,
    VALIDATED,
    PAYMENT_INITIATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
