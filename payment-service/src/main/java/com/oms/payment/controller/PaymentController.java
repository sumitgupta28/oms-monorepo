package com.oms.payment.controller;

import com.oms.payment.dto.InitiatePaymentRequest;
import com.oms.payment.dto.PaymentResponse;
import com.oms.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> initiatePayment(
        @Valid @RequestBody InitiatePaymentRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt
    ) {
        PaymentResponse resp = paymentService.initiatePayment(
            request.orderId(), jwt.getSubject(), request.amount(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public PaymentResponse getStatus(@PathVariable UUID paymentId) {
        return paymentService.getStatus(paymentId);
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse refund(@PathVariable UUID paymentId) {
        return paymentService.refund(paymentId);
    }
}