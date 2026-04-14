package com.oms.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.events.OrderCancelledEvent;
import com.oms.events.OrderPlacedEvent;
import com.oms.events.OrderShippedEvent;
import com.oms.order.domain.Order;
import com.oms.order.domain.OrderItem;
import com.oms.order.domain.OrderStatus;
import com.oms.order.domain.OutboxEvent;
import com.oms.order.exception.OrderNotFoundException;
import com.oms.order.exception.OrderStateException;
import com.oms.order.repository.OrderRepository;
import com.oms.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository        orderRepository;
    private final OutboxEventRepository  outboxRepository;
    private final ObjectMapper           objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // ── Topics ─────────────────────────────────────────────────────────────
    private static final String TOPIC_PLACED    = "oms.orders.placed";
    private static final String TOPIC_CANCELLED = "oms.orders.cancelled";
    private static final String TOPIC_SHIPPED   = "oms.orders.shipped";

    // ── Place Order ─────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse placeOrder(String userId, String userEmail,
                                    PlaceOrderRequest request) {
        BigDecimal total = request.items().stream()
            .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
            .userId(userId)
            .userEmail(userEmail)
            .status(OrderStatus.PENDING)
            .totalAmount(total)
            .build();

        request.items().forEach(i -> {
            OrderItem item = OrderItem.builder()
                .productId(i.productId())
                .productName(i.productName())
                .quantity(i.quantity())
                .unitPrice(i.unitPrice())
                .build();
            order.addItem(item);
        });

        Order saved = orderRepository.save(order);

        // Write to outbox (transactional — same DB commit)
        var event = new OrderPlacedEvent(
            saved.getId(), userId,
            request.items().stream()
                .map(i -> new OrderPlacedEvent.OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList(),
            total
        );
        saveOutbox(event, TOPIC_PLACED);

        log.info("Order placed: {} for user {}", saved.getId(), userId);
        return OrderResponse.from(saved);
    }

    // ── Get Orders ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userId) {
        return orderRepository.findByUserIdWithItems(userId)
            .stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
            .stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID orderId, String userId, boolean isAdmin) {
        Order order = findOrder(orderId);
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new OrderStateException("Access denied to order " + orderId);
        }
        return OrderResponse.from(order);
    }

    // ── Cancel Order ────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String userId, String reason) {
        Order order = findOrder(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new OrderStateException("Cannot cancel another user's order");
        }
        if (!order.isCancellable()) {
            throw new OrderStateException(
                "Order " + orderId + " cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        Order saved = orderRepository.save(order);

        saveOutbox(new OrderCancelledEvent(orderId, userId, reason), TOPIC_CANCELLED);
        log.info("Order cancelled: {} reason: {}", orderId, reason);
        return OrderResponse.from(saved);
    }

    // ── Ship Order (Admin) ──────────────────────────────────────────────────
    @Transactional
    public OrderResponse shipOrder(UUID orderId, String trackingNumber) {
        Order order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderStateException(
                "Order must be PAID before shipping. Current: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        Order saved = orderRepository.save(order);

        saveOutbox(new OrderShippedEvent(orderId, order.getUserId(), trackingNumber), TOPIC_SHIPPED);
        log.info("Order shipped: {} tracking: {}", orderId, trackingNumber);
        return OrderResponse.from(saved);
    }

    // ── Advance to PAID (called by Kafka consumer) ──────────────────────────
    @Transactional
    public void markPaid(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.PAYMENT_INITIATED) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Order marked PAID: {}", orderId);
        }
    }

    // ── Advance to VALIDATED (called by Kafka consumer) ─────────────────────
    @Transactional
    public void markValidated(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.VALIDATED);
            orderRepository.save(order);
            log.info("Order marked VALIDATED: {}", orderId);
        }
    }

    // ── Outbox Publisher ─────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()
            .forEach(event -> {
                try {
                    kafkaTemplate.send(event.getTopic(), event.getPayload());
                    event.setPublished(true);
                    event.setPublishedAt(Instant.now());
                    outboxRepository.save(event);
                    log.debug("Published outbox event: {} to {}", event.getEventType(), event.getTopic());
                } catch (Exception ex) {
                    log.error("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
                }
            });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Order findOrder(UUID orderId) {
        return orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private void saveOutbox(Object event, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.builder()
                .eventType(event.getClass().getSimpleName())
                .topic(topic)
                .payload(payload)
                .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
