package com.oms.order.service;

import jakarta.validation.constraints.NotBlank;

public record ShipOrderRequest(
    @NotBlank(message = "Tracking number is required")
    String trackingNumber
) {}
