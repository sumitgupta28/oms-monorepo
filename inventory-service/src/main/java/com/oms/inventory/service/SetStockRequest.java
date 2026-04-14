package com.oms.inventory.service;

public record SetStockRequest(String productId, int quantity) {
}
