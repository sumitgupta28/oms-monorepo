package com.oms.order.service;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
    @NotBlank(message = "Cancellation reason is required")
    String reason
) {}
