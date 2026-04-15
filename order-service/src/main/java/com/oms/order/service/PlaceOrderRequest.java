package com.oms.order.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceOrderRequest(
    @NotEmpty(message = "Order must contain at least one item")
    List<@Valid OrderItemRequest> items
) {}
