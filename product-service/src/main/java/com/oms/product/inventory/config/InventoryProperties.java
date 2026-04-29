package com.oms.product.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory")
public record InventoryProperties(int lowStockThreshold) {
    public InventoryProperties {
        if (lowStockThreshold < 0) throw new IllegalArgumentException("lowStockThreshold must be >= 0");
    }
}
