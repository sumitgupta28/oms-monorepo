package com.oms.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.inventory.service.InventoryService;
import com.oms.inventory.service.InventoryService.ReserveItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryEventConsumer")
class InventoryEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryEventConsumer consumer;

    @Captor
    private ArgumentCaptor<UUID> orderIdCaptor;

    @Captor
    private ArgumentCaptor<List<ReserveItem>> itemsCaptor;

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(consumer, "objectMapper", objectMapper);
    }

    @Nested
    @DisplayName("onOrderPlaced")
    class OnOrderPlaced {

        @Test
        void shouldParseEventAndCallReserveStock() {
            var payload = """
                {
                  "orderId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "items": [
                    {"productId": "prod-1", "quantity": 3}
                  ]
                }
                """;

            consumer.onOrderPlaced(payload);

            verify(inventoryService).reserveStock(orderIdCaptor.capture(), itemsCaptor.capture());
            assertThat(orderIdCaptor.getValue()).isEqualTo(ORDER_ID);
            assertThat(itemsCaptor.getValue()).hasSize(1);
            assertThat(itemsCaptor.getValue().getFirst().productId()).isEqualTo("prod-1");
            assertThat(itemsCaptor.getValue().getFirst().quantity()).isEqualTo(3);
        }

        @Test
        void shouldParseMultipleItems() {
            var payload = """
                {
                  "orderId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "items": [
                    {"productId": "prod-1", "quantity": 2},
                    {"productId": "prod-2", "quantity": 5},
                    {"productId": "prod-3", "quantity": 1}
                  ]
                }
                """;

            consumer.onOrderPlaced(payload);

            verify(inventoryService).reserveStock(eq(ORDER_ID), itemsCaptor.capture());
            var items = itemsCaptor.getValue();
            assertThat(items).hasSize(3);
            assertThat(items).extracting(ReserveItem::productId)
                .containsExactly("prod-1", "prod-2", "prod-3");
            assertThat(items).extracting(ReserveItem::quantity)
                .containsExactly(2, 5, 1);
        }

        @Test
        void shouldHandleMalformedJsonWithoutThrowing() {
            consumer.onOrderPlaced("not valid json {{{");

            verifyNoInteractions(inventoryService);
        }

        @Test
        void shouldHandleMissingOrderIdField() {
            var payload = """
                {
                  "items": [
                    {"productId": "prod-1", "quantity": 3}
                  ]
                }
                """;

            consumer.onOrderPlaced(payload);

            verifyNoInteractions(inventoryService);
        }

        @Test
        void shouldHandleMissingItemsField() {
            var payload = """
                {
                  "orderId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                }
                """;

            consumer.onOrderPlaced(payload);

            verifyNoInteractions(inventoryService);
        }

        @Test
        void shouldHandleInvalidUuidFormat() {
            var payload = """
                {
                  "orderId": "not-a-uuid",
                  "items": [
                    {"productId": "prod-1", "quantity": 3}
                  ]
                }
                """;

            consumer.onOrderPlaced(payload);

            verifyNoInteractions(inventoryService);
        }

        @Test
        void shouldHandleEmptyPayload() {
            consumer.onOrderPlaced("");

            verifyNoInteractions(inventoryService);
        }
    }

    @Nested
    @DisplayName("onOrderCancelled")
    class OnOrderCancelled {

        @Test
        void shouldParseEventAndCallReleaseStockByOrderId() {
            var payload = """
                {
                  "orderId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                }
                """;

            consumer.onOrderCancelled(payload);

            verify(inventoryService).releaseStockByOrderId(ORDER_ID);
        }

        @Test
        void shouldHandleMalformedJsonWithoutThrowing() {
            consumer.onOrderCancelled("{{bad json}}");

            verifyNoInteractions(inventoryService);
        }

        @Test
        void shouldHandleMissingOrderIdField() {
            var payload = """
                {
                  "status": "CANCELLED"
                }
                """;

            consumer.onOrderCancelled(payload);

            verifyNoInteractions(inventoryService);
        }

        @Test
        void shouldHandleInvalidUuidFormat() {
            var payload = """
                {
                  "orderId": "invalid-uuid-format"
                }
                """;

            consumer.onOrderCancelled(payload);

            verifyNoInteractions(inventoryService);
        }

        @Test
        void shouldHandleEmptyPayload() {
            consumer.onOrderCancelled("");

            verifyNoInteractions(inventoryService);
        }
    }
}
