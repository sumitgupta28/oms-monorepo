package com.oms.order.controller;

import com.oms.order.dto.CancelOrderRequest;
import com.oms.order.dto.OrderResponse;
import com.oms.order.dto.PlaceOrderRequest;
import com.oms.order.dto.ShipOrderRequest;
import com.oms.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication
    ) {
        return orderService.getById(orderId, jwt.getSubject(), isAdmin(authentication));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderResponse> getAllOrders(
        @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return orderService.getAllOrders(pageable);
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public OrderResponse cancelOrder(
        @PathVariable UUID orderId,
        @Valid @RequestBody CancelOrderRequest request,
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication
    ) {
        return orderService.cancelOrder(orderId, jwt.getSubject(), isAdmin(authentication), request.reason());
    }

    @PatchMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse shipOrder(
        @PathVariable UUID orderId,
        @Valid @RequestBody ShipOrderRequest request
    ) {
        return orderService.shipOrder(orderId, request.trackingNumber());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
