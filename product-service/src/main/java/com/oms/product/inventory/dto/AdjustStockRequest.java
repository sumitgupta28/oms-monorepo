package com.oms.product.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
    @NotNull int delta,
    @NotBlank(message = "Adjustment reason is required") String reason
) {}
