package com.oms.payment.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentRequest(
    @NotNull(message = "Order ID is required")
    UUID orderId,
    String userId,
    @NotNull @Positive(message = "Amount must be positive")
    BigDecimal amount,
    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey
) {}
