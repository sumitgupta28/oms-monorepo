package com.oms.order.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(
    @NotBlank(message = "Product ID is required")
    String productId,
    @NotBlank(message = "Product name is required")
    String productName,
    @Positive(message = "Quantity must be positive")
    int quantity,
    @NotNull @Positive(message = "Unit price must be positive")
    BigDecimal unitPrice
) {}
