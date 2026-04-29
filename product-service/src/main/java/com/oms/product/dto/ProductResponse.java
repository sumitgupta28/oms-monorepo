package com.oms.product.dto;

import com.oms.product.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(String id, String name, String description, String category,
                              BigDecimal price, int stockQty, String imageUrl, boolean active, Instant createdAt) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getCategory(),
                p.getPrice(), p.getStockQty(), p.getImageUrl(), p.isActive(), p.getCreatedAt());
    }
}
