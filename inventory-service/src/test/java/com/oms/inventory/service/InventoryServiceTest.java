package com.oms.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.inventory.domain.Inventory;
import com.oms.inventory.domain.StockMovement;
import com.oms.inventory.exception.InventoryException;
import com.oms.inventory.repository.InventoryRepository;
import com.oms.inventory.repository.StockMovementRepository;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepo;

    @Mock
    private StockMovementRepository movementRepo;

    @Mock
    private KafkaTemplate<String, String> kafka;

    @InjectMocks
    private InventoryService inventoryService;

    @Captor
    private ArgumentCaptor<StockMovement> movementCaptor;

    @Captor
    private ArgumentCaptor<Inventory> inventoryCaptor;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    private static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PRODUCT_A = "product-a";
    private static final String PRODUCT_B = "product-b";

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(inventoryService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(inventoryService, "lowStockThreshold", 5);
    }

    @Nested
    @DisplayName("reserveStock")
    class ReserveStock {

        @Test
        void shouldReserveStockAndPublishReservedEvent() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(20).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            var items = List.of(new ReserveItem(PRODUCT_A, 3));

            inventoryService.reserveStock(ORDER_ID, items);

            assertThat(inv.getAvailableQty()).isEqualTo(17);
            assertThat(inv.getReservedQty()).isEqualTo(3);
            verify(inventoryRepo).save(inv);
            verify(movementRepo).save(argThat(m ->
                m.getProductId().equals(PRODUCT_A) &&
                m.getMovementType() == StockMovement.MovementType.RESERVE &&
                m.getDelta() == -3 &&
                m.getOrderId().equals(ORDER_ID)
            ));
            verify(kafka).send(eq("oms.inventory.reserved"), contains("INVENTORY_RESERVED"));
        }

        @Test
        void shouldReserveMultipleItemsAndPublishSingleReservedEvent() {
            var invA = Inventory.builder().productId(PRODUCT_A).availableQty(10).reservedQty(0).build();
            var invB = Inventory.builder().productId(PRODUCT_B).availableQty(15).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(invA));
            when(inventoryRepo.findByIdWithLock(PRODUCT_B)).thenReturn(Optional.of(invB));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            var items = List.of(new ReserveItem(PRODUCT_A, 2), new ReserveItem(PRODUCT_B, 5));

            inventoryService.reserveStock(ORDER_ID, items);

            assertThat(invA.getAvailableQty()).isEqualTo(8);
            assertThat(invB.getAvailableQty()).isEqualTo(10);
            verify(inventoryRepo, times(2)).save(any());
            verify(movementRepo, times(2)).save(any());
            verify(kafka).send(eq("oms.inventory.reserved"), argThat(payload ->
                payload.contains("INVENTORY_RESERVED") &&
                payload.contains(PRODUCT_A) &&
                payload.contains(PRODUCT_B)
            ));
        }

        @Test
        void shouldPublishInsufficientEventAndReturnEarlyWhenStockInsufficient() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(2).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));

            inventoryService.reserveStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 5)));

            assertThat(inv.getAvailableQty()).isEqualTo(2);
            assertThat(inv.getReservedQty()).isZero();
            verify(inventoryRepo, never()).save(any());
            verify(kafka).send(eq("oms.inventory.insufficient"), contains("INVENTORY_INSUFFICIENT"));
            verify(kafka, never()).send(eq("oms.inventory.reserved"), anyString());
        }

        @Test
        void shouldThrowInventoryExceptionWhenProductNotFound() {
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                inventoryService.reserveStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 1)))
            )
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Product not found: " + PRODUCT_A);
        }

        @Test
        void shouldPublishLowStockEventWhenAvailableQtyEqualsThreshold() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(8).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            inventoryService.reserveStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 3)));

            assertThat(inv.getAvailableQty()).isEqualTo(5);
            verify(kafka).send(eq("oms.inventory.low-stock"), contains(PRODUCT_A));
            verify(kafka).send(eq("oms.inventory.reserved"), anyString());
        }

        @Test
        void shouldPublishLowStockEventWhenAvailableQtyBelowThreshold() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(6).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            inventoryService.reserveStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 3)));

            assertThat(inv.getAvailableQty()).isEqualTo(3);
            verify(kafka).send(eq("oms.inventory.low-stock"), contains(PRODUCT_A));
        }

        @Test
        void shouldNotPublishLowStockEventWhenAvailableQtyAboveThreshold() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(20).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            inventoryService.reserveStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 2)));

            assertThat(inv.getAvailableQty()).isEqualTo(18);
            verify(kafka, never()).send(eq("oms.inventory.low-stock"), anyString());
        }

        @Test
        void shouldStopProcessingRemainingItemsAfterInsufficientStock() {
            var invA = Inventory.builder().productId(PRODUCT_A).availableQty(1).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(invA));
            var items = List.of(new ReserveItem(PRODUCT_A, 5), new ReserveItem(PRODUCT_B, 2));

            inventoryService.reserveStock(ORDER_ID, items);

            verify(inventoryRepo, never()).findByIdWithLock(PRODUCT_B);
            verify(kafka).send(eq("oms.inventory.insufficient"), anyString());
            verify(kafka, never()).send(eq("oms.inventory.reserved"), anyString());
        }

        @Test
        void shouldRecordCorrectMovementDelta() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(50).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            inventoryService.reserveStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 7)));

            verify(movementRepo).save(movementCaptor.capture());
            var movement = movementCaptor.getValue();
            assertThat(movement.getDelta()).isEqualTo(-7);
            assertThat(movement.getMovementType()).isEqualTo(StockMovement.MovementType.RESERVE);
            assertThat(movement.getOrderId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("releaseStock")
    class ReleaseStock {

        @Test
        void shouldReleaseStockAndRecordMovement() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(5).reservedQty(10).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            inventoryService.releaseStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 3)));

            assertThat(inv.getAvailableQty()).isEqualTo(8);
            assertThat(inv.getReservedQty()).isEqualTo(7);
            verify(inventoryRepo).save(inv);
            verify(movementRepo).save(argThat(m ->
                m.getProductId().equals(PRODUCT_A) &&
                m.getMovementType() == StockMovement.MovementType.RELEASE &&
                m.getDelta() == 3 &&
                m.getOrderId().equals(ORDER_ID)
            ));
        }

        @Test
        void shouldDoNothingWhenProductNotFound() {
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.empty());

            inventoryService.releaseStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 3)));

            verify(inventoryRepo, never()).save(any());
            verify(movementRepo, never()).save(any());
        }

        @Test
        void shouldReleaseMultipleItems() {
            var invA = Inventory.builder().productId(PRODUCT_A).availableQty(0).reservedQty(5).build();
            var invB = Inventory.builder().productId(PRODUCT_B).availableQty(0).reservedQty(10).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(invA));
            when(inventoryRepo.findByIdWithLock(PRODUCT_B)).thenReturn(Optional.of(invB));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            var items = List.of(new ReserveItem(PRODUCT_A, 5), new ReserveItem(PRODUCT_B, 7));

            inventoryService.releaseStock(ORDER_ID, items);

            assertThat(invA.getAvailableQty()).isEqualTo(5);
            assertThat(invB.getAvailableQty()).isEqualTo(7);
            verify(inventoryRepo, times(2)).save(any());
            verify(movementRepo, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("releaseStockByOrderId")
    class ReleaseStockByOrderId {

        @Test
        void shouldFindReservationsAndReleaseAbsDelta() {
            var reservation = StockMovement.builder()
                .productId(PRODUCT_A)
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-4)
                .orderId(ORDER_ID)
                .build();
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(0).reservedQty(4).build();
            when(movementRepo.findByOrderIdAndMovementType(ORDER_ID, StockMovement.MovementType.RESERVE))
                .thenReturn(List.of(reservation));
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            inventoryService.releaseStockByOrderId(ORDER_ID);

            assertThat(inv.getAvailableQty()).isEqualTo(4);
            assertThat(inv.getReservedQty()).isZero();
            verify(inventoryRepo).save(inv);
            verify(movementRepo).save(argThat(m ->
                m.getMovementType() == StockMovement.MovementType.RELEASE &&
                m.getDelta() == 4 &&
                m.getOrderId().equals(ORDER_ID)
            ));
        }

        @Test
        void shouldHandleMultipleReservationsForSameOrder() {
            var res1 = StockMovement.builder()
                .productId(PRODUCT_A).movementType(StockMovement.MovementType.RESERVE)
                .delta(-3).orderId(ORDER_ID).build();
            var res2 = StockMovement.builder()
                .productId(PRODUCT_B).movementType(StockMovement.MovementType.RESERVE)
                .delta(-5).orderId(ORDER_ID).build();
            var invA = Inventory.builder().productId(PRODUCT_A).availableQty(0).reservedQty(3).build();
            var invB = Inventory.builder().productId(PRODUCT_B).availableQty(0).reservedQty(5).build();
            when(movementRepo.findByOrderIdAndMovementType(ORDER_ID, StockMovement.MovementType.RESERVE))
                .thenReturn(List.of(res1, res2));
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(invA));
            when(inventoryRepo.findByIdWithLock(PRODUCT_B)).thenReturn(Optional.of(invB));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            inventoryService.releaseStockByOrderId(ORDER_ID);

            assertThat(invA.getAvailableQty()).isEqualTo(3);
            assertThat(invB.getAvailableQty()).isEqualTo(5);
            verify(inventoryRepo, times(2)).save(any());
            verify(movementRepo, times(2)).save(any());
        }

        @Test
        void shouldDoNothingWhenNoPriorReservationsExist() {
            when(movementRepo.findByOrderIdAndMovementType(ORDER_ID, StockMovement.MovementType.RESERVE))
                .thenReturn(List.of());

            inventoryService.releaseStockByOrderId(ORDER_ID);

            verify(inventoryRepo, never()).findByIdWithLock(anyString());
            verify(inventoryRepo, never()).save(any());
            verify(movementRepo, never()).save(any());
        }

        @Test
        void shouldSkipWhenInventoryNotFoundForReservation() {
            var reservation = StockMovement.builder()
                .productId(PRODUCT_A).movementType(StockMovement.MovementType.RESERVE)
                .delta(-3).orderId(ORDER_ID).build();
            when(movementRepo.findByOrderIdAndMovementType(ORDER_ID, StockMovement.MovementType.RESERVE))
                .thenReturn(List.of(reservation));
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.empty());

            inventoryService.releaseStockByOrderId(ORDER_ID);

            verify(inventoryRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setStock")
    class SetStock {

        @Test
        void shouldUpdateExistingInventory() {
            var existing = Inventory.builder().productId(PRODUCT_A).availableQty(10).reservedQty(3).build();
            when(inventoryRepo.findById(PRODUCT_A)).thenReturn(Optional.of(existing));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = inventoryService.setStock(PRODUCT_A, 50);

            assertThat(result.productId()).isEqualTo(PRODUCT_A);
            assertThat(result.availableQty()).isEqualTo(50);
            assertThat(result.reservedQty()).isEqualTo(3);
            verify(inventoryRepo).save(inventoryCaptor.capture());
            assertThat(inventoryCaptor.getValue().getAvailableQty()).isEqualTo(50);
        }

        @Test
        void shouldCreateNewInventoryWhenProductDoesNotExist() {
            when(inventoryRepo.findById(PRODUCT_A)).thenReturn(Optional.empty());
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = inventoryService.setStock(PRODUCT_A, 25);

            assertThat(result.productId()).isEqualTo(PRODUCT_A);
            assertThat(result.availableQty()).isEqualTo(25);
            assertThat(result.reservedQty()).isZero();
            verify(inventoryRepo).save(any(Inventory.class));
        }

        @Test
        void shouldSetStockToZero() {
            var existing = Inventory.builder().productId(PRODUCT_A).availableQty(10).build();
            when(inventoryRepo.findById(PRODUCT_A)).thenReturn(Optional.of(existing));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = inventoryService.setStock(PRODUCT_A, 0);

            assertThat(result.availableQty()).isZero();
        }
    }

    @Nested
    @DisplayName("adjustStock")
    class AdjustStock {

        @Test
        void shouldAddDeltaAndRecordRestockMovement() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(10).reservedQty(2).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = inventoryService.adjustStock(PRODUCT_A, 15);

            assertThat(result.availableQty()).isEqualTo(25);
            assertThat(result.reservedQty()).isEqualTo(2);
            verify(movementRepo).save(argThat(m ->
                m.getMovementType() == StockMovement.MovementType.RESTOCK &&
                m.getDelta() == 15 &&
                m.getProductId().equals(PRODUCT_A)
            ));
        }

        @Test
        void shouldHandleNegativeDelta() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(20).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = inventoryService.adjustStock(PRODUCT_A, -5);

            assertThat(result.availableQty()).isEqualTo(15);
            verify(movementRepo).save(argThat(m -> m.getDelta() == -5));
        }

        @Test
        void shouldThrowWhenProductNotFound() {
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.adjustStock(PRODUCT_A, 10))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Product not found: " + PRODUCT_A);
        }

        @Test
        void shouldReturnInventoryResponseWithCorrectFields() {
            var inv = Inventory.builder()
                .productId(PRODUCT_A).availableQty(10).reservedQty(3)
                .updatedAt(Instant.parse("2025-01-01T00:00:00Z")).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = inventoryService.adjustStock(PRODUCT_A, 5);

            assertThat(result.productId()).isEqualTo(PRODUCT_A);
            assertThat(result.availableQty()).isEqualTo(15);
            assertThat(result.reservedQty()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getStock")
    class GetStock {

        @Test
        void shouldReturnResponseWhenInventoryExists() {
            var inv = Inventory.builder()
                .productId(PRODUCT_A).availableQty(10).reservedQty(3)
                .updatedAt(Instant.parse("2025-06-01T12:00:00Z")).build();
            when(inventoryRepo.findById(PRODUCT_A)).thenReturn(Optional.of(inv));

            var result = inventoryService.getStock(PRODUCT_A);

            assertThat(result.productId()).isEqualTo(PRODUCT_A);
            assertThat(result.availableQty()).isEqualTo(10);
            assertThat(result.reservedQty()).isEqualTo(3);
            assertThat(result.updatedAt()).isEqualTo(Instant.parse("2025-06-01T12:00:00Z"));
        }

        @Test
        void shouldReturnZeroQuantityDefaultWhenNotFound() {
            when(inventoryRepo.findById(PRODUCT_A)).thenReturn(Optional.empty());

            var result = inventoryService.getStock(PRODUCT_A);

            assertThat(result.productId()).isEqualTo(PRODUCT_A);
            assertThat(result.availableQty()).isZero();
            assertThat(result.reservedQty()).isZero();
            assertThat(result.updatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("getAllStock")
    class GetAllStock {

        @Test
        void shouldReturnAllInventoryAsMappedResponses() {
            var invA = Inventory.builder().productId(PRODUCT_A).availableQty(10).reservedQty(2).build();
            var invB = Inventory.builder().productId(PRODUCT_B).availableQty(20).reservedQty(5).build();
            when(inventoryRepo.findAll()).thenReturn(List.of(invA, invB));

            var result = inventoryService.getAllStock();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(InventoryResponse::productId)
                .containsExactly(PRODUCT_A, PRODUCT_B);
            assertThat(result).extracting(InventoryResponse::availableQty)
                .containsExactly(10, 20);
        }

        @Test
        void shouldReturnEmptyListWhenNoInventory() {
            when(inventoryRepo.findAll()).thenReturn(List.of());

            var result = inventoryService.getAllStock();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("publishEvent error handling")
    class PublishEventErrorHandling {

        @Test
        void shouldNotThrowWhenKafkaSendFails() {
            var inv = Inventory.builder().productId(PRODUCT_A).availableQty(20).reservedQty(0).build();
            when(inventoryRepo.findByIdWithLock(PRODUCT_A)).thenReturn(Optional.of(inv));
            when(inventoryRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(kafka.send(anyString(), anyString())).thenThrow(new RuntimeException("Kafka down"));

            inventoryService.reserveStock(ORDER_ID, List.of(new ReserveItem(PRODUCT_A, 1)));

            assertThat(inv.getAvailableQty()).isEqualTo(19);
        }
    }
}
