package com.oms.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.payment.domain.Payment;
import com.oms.payment.domain.PaymentLedger;
import com.oms.payment.domain.PaymentStatus;
import com.oms.payment.repository.PaymentLedgerRepository;
import com.oms.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository        paymentRepository;
    private final PaymentLedgerRepository  ledgerRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper             objectMapper;
    private final MockGateway              mockGateway;

    private static final String TOPIC_CONFIRMED = "oms.payment.confirmed";
    private static final String TOPIC_FAILED    = "oms.payment.failed";

    @Transactional
    public PaymentResponse initiatePayment(UUID orderId, String userId,
                                           BigDecimal amount, String idempotencyKey) {
        // Idempotency check
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
            .map(existing -> {
                log.info("Duplicate payment request for key: {}", idempotencyKey);
                return PaymentResponse.from(existing);
            })
            .orElseGet(() -> {
                Payment payment = Payment.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .amount(amount)
                    .status(PaymentStatus.PROCESSING)
                    .idempotencyKey(idempotencyKey)
                    .build();
                Payment saved = paymentRepository.save(payment);
                processAsync(saved);
                return PaymentResponse.from(saved);
            });
    }

    private void processAsync(Payment payment) {
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(mockGateway.getDelayMs());
                boolean success = mockGateway.process();
                updatePaymentResult(payment.getId(), success);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Transactional
    public void updatePaymentResult(UUID paymentId, boolean success) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            if (success) {
                payment.setStatus(PaymentStatus.CONFIRMED);
                payment.setProcessedAt(Instant.now());
                paymentRepository.save(payment);

                ledgerRepository.save(PaymentLedger.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .entryType(PaymentLedger.EntryType.CHARGE)
                    .amount(payment.getAmount())
                    .build());

                publishEvent(TOPIC_CONFIRMED, Map.of(
                    "eventType", "PAYMENT_CONFIRMED",
                    "paymentId", payment.getId().toString(),
                    "orderId",   payment.getOrderId().toString(),
                    "amount",    payment.getAmount().toString()
                ));
                log.info("Payment confirmed: {}", paymentId);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Mock gateway rejection");
                payment.setProcessedAt(Instant.now());
                paymentRepository.save(payment);

                publishEvent(TOPIC_FAILED, Map.of(
                    "eventType", "PAYMENT_FAILED",
                    "orderId",   payment.getOrderId().toString(),
                    "reason",    "Mock gateway rejection"
                ));
                log.warn("Payment failed: {}", paymentId);
            }
        });
    }

    @Transactional
    public PaymentResponse refund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new com.oms.payment.exception.PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new com.oms.payment.exception.PaymentStateException("Only CONFIRMED payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        ledgerRepository.save(PaymentLedger.builder()
            .paymentId(payment.getId())
            .orderId(payment.getOrderId())
            .entryType(PaymentLedger.EntryType.REFUND)
            .amount(payment.getAmount().negate())
            .build());

        log.info("Payment refunded: {}", paymentId);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getStatus(UUID paymentId) {
        return paymentRepository.findById(paymentId)
            .map(PaymentResponse::from)
            .orElseThrow(() -> new com.oms.payment.exception.PaymentNotFoundException("Payment not found: " + paymentId));
    }

    private void publishEvent(String topic, Map<String, String> payload) {
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to publish to {}: {}", topic, e.getMessage());
        }
    }
}