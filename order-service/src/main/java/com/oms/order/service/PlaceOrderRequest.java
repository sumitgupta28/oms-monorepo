package com.oms.order.service;

import java.util.List;

public record PlaceOrderRequest(
    List<OrderItemRequest> items
) {}

