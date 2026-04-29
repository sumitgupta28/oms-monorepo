package com.oms.product.inventory.dto;

import com.oms.product.inventory.domain.Inventory;

import java.time.Instant;

public record InventoryResponse(
    String productId,
    int availableQty,
    int reservedQty,
    Instant updatedAt
) {
    public static InventoryResponse from(Inventory i) {
        return new InventoryResponse(i.getProductId(), i.getAvailableQty(),
            i.getReservedQty(), i.getUpdatedAt());
    }
}
