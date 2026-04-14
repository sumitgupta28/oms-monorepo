package com.oms.product.service;

import java.math.BigDecimal;

public record CreateProductRequest(String name, String description, String category, BigDecimal price, int stockQty,
                                   String imageUrl) {
}
