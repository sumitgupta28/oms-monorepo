package com.oms.order.controller;

import com.oms.order.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> placeOrder(
        @Valid @RequestBody PlaceOrderRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String userId    = jwt.getSubject();
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.placeOrder(userId, userEmail, request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getMyOrders(jwt.getSubject());
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public OrderResponse getOrder(
        @PathVariable UUID orderId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.getById(orderId, jwt.getSubject(), isAdmin(jwt));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public OrderResponse cancelOrder(
        @PathVariable UUID orderId,
        @Valid @RequestBody CancelOrderRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.cancelOrder(orderId, jwt.getSubject(), request.reason());
    }

    @PatchMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse shipOrder(
        @PathVariable UUID orderId,
        @Valid @RequestBody ShipOrderRequest request
    ) {
        return orderService.shipOrder(orderId, request.trackingNumber());
    }

    private boolean isAdmin(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        @SuppressWarnings("unchecked")
        var roles = (java.util.List<String>) realmAccess.get("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
