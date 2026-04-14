package com.oms.order.controller;

import com.oms.order.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ── Place a new order ────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> placeOrder(
        @RequestBody PlaceOrderRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String userId    = jwt.getSubject();
        String userEmail = jwt.getClaimAsString("email");
        OrderResponse response = orderService.placeOrder(userId, userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Get my orders (customer) ─────────────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getMyOrders(jwt.getSubject());
    }

    // ── Get single order ─────────────────────────────────────────────────────
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public OrderResponse getOrder(
        @PathVariable UUID orderId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        boolean isAdmin = jwt.getClaimAsStringList("realm_access") != null
            && hasAdminRole(jwt);
        return orderService.getById(orderId, jwt.getSubject(), isAdmin);
    }

    // ── Get all orders (admin only) ───────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    // ── Cancel order ─────────────────────────────────────────────────────────
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public OrderResponse cancelOrder(
        @PathVariable UUID orderId,
        @RequestBody CancelOrderRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.cancelOrder(orderId, jwt.getSubject(), request.reason());
    }

    // ── Ship order (admin only) ───────────────────────────────────────────────
    @PatchMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse shipOrder(
        @PathVariable UUID orderId,
        @RequestBody ShipOrderRequest request
    ) {
        return orderService.shipOrder(orderId, request.trackingNumber());
    }

    private boolean hasAdminRole(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        var roles = (java.util.List<?>) realmAccess.get("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
