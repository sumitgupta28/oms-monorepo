package com.oms.payment.service;

import com.oms.payment.domain.Payment;
import com.oms.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    String userId,
    BigDecimal amount,
    PaymentStatus status,
    String idempotencyKey,
    String failureReason,
    Instant createdAt,
    Instant processedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getUserId(),
            p.getAmount(), p.getStatus(), p.getIdempotencyKey(),
            p.getFailureReason(), p.getCreatedAt(), p.getProcessedAt());
    }
}