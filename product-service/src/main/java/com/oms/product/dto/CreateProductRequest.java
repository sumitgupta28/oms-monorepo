package com.oms.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank(message = "Product name is required")
    String name,
    @NotBlank(message = "Description is required")
    String description,
    @NotBlank(message = "Category is required")
    String category,
    @NotNull @Positive(message = "Price must be positive")
    BigDecimal price,
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    int stockQty,
    String imageUrl
) {}
