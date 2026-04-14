package com.oms.inventory.service;

public record AdjustStockRequest(int delta, String reason){}
