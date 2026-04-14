package com.oms.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentRequest(
    UUID orderId,
    String userId,
    BigDecimal amount,
    String idempotencyKey
) {}