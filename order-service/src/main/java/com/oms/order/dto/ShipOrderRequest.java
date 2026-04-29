package com.oms.order.dto;

import jakarta.validation.constraints.NotBlank;

public record ShipOrderRequest(
    @NotBlank(message = "Tracking number is required")
    String trackingNumber
) {}
