package com.oms.order.service;

import java.math.BigDecimal;

public record OrderItemRequest(
    String productId,
    String productName,
    int quantity,
    BigDecimal unitPrice
) {}

