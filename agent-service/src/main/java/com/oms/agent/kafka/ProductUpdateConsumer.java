package com.oms.agent.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.agent.embedding.ProductIndexingService;
import com.oms.events.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductUpdateConsumer {

    private final ProductIndexingService productIndexingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "oms.products.updated", groupId = "agent-service-indexer")
    public void onProductUpdated(String payload) {
        try {
            ProductUpdatedEvent event = objectMapper.readValue(payload, ProductUpdatedEvent.class);
            log.info("Received product.updated event for productId={}", event.productId());
            productIndexingService.indexProduct(event.productId().toString());
        } catch (Exception e) {
            log.warn("Failed to process product.updated event: {}", e.getMessage());
        }
    }
}
