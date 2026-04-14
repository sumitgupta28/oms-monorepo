package com.oms.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.notification.sender.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class PaymentEventConsumer {
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics="oms.payment.confirmed", groupId="oms-notification-group")
    public void onPaymentConfirmed(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            log.info("Payment confirmed for order {}", n.path("orderId").asText());
        } catch (Exception e) { log.error("onPaymentConfirmed error: {}", e.getMessage()); }
    }
}
