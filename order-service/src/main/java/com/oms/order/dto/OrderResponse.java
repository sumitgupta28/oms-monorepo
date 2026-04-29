package com.oms.order.dto;

import com.oms.order.domain.Order;
import com.oms.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String userId,
    String userEmail,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items,
    String trackingNumber,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getUserId(),
            order.getUserEmail(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getItems().stream().map(OrderItemResponse::from).toList(),
            order.getTrackingNumber(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
