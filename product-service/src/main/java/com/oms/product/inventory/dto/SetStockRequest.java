package com.oms.product.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SetStockRequest(
    @NotBlank(message = "Product ID is required") String productId,
    @PositiveOrZero(message = "Quantity cannot be negative") int quantity
) {}
