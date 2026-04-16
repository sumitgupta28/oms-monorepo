package com.oms.inventory.repository;

import com.oms.inventory.domain.Inventory;
import com.oms.inventory.domain.StockMovement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Repository integration tests")
class InventoryRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("inventory_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("InventoryRepository")
    class InventoryRepoTests {

        @Test
        void shouldSaveAndFindById() {
            var inventory = Inventory.builder()
                .productId("prod-100")
                .availableQty(50)
                .reservedQty(10)
                .build();

            inventoryRepository.save(inventory);
            entityManager.flush();
            entityManager.clear();

            var found = inventoryRepository.findById("prod-100");

            assertThat(found).isPresent();
            assertThat(found.get().getAvailableQty()).isEqualTo(50);
            assertThat(found.get().getReservedQty()).isEqualTo(10);
            assertThat(found.get().getUpdatedAt()).isNotNull();
        }

        @Test
        void shouldReturnEmptyOptionalWhenNotFound() {
            var found = inventoryRepository.findById("nonexistent-product");

            assertThat(found).isEmpty();
        }

        @Test
        void shouldFindByIdWithLockWhenExists() {
            var inventory = Inventory.builder()
                .productId("prod-lock-1")
                .availableQty(25)
                .reservedQty(5)
                .build();
            inventoryRepository.save(inventory);
            entityManager.flush();
            entityManager.clear();

            var found = inventoryRepository.findByIdWithLock("prod-lock-1");

            assertThat(found).isPresent();
            assertThat(found.get().getProductId()).isEqualTo("prod-lock-1");
            assertThat(found.get().getAvailableQty()).isEqualTo(25);
        }

        @Test
        void shouldReturnEmptyFromFindByIdWithLockWhenNotExists() {
            var found = inventoryRepository.findByIdWithLock("missing-product");

            assertThat(found).isEmpty();
        }

        @Test
        void shouldUpdateExistingInventory() {
            var inventory = Inventory.builder()
                .productId("prod-update")
                .availableQty(10)
                .reservedQty(0)
                .build();
            inventoryRepository.save(inventory);
            entityManager.flush();
            entityManager.clear();

            var loaded = inventoryRepository.findById("prod-update").orElseThrow();
            loaded.setAvailableQty(99);
            inventoryRepository.save(loaded);
            entityManager.flush();
            entityManager.clear();

            var updated = inventoryRepository.findById("prod-update").orElseThrow();
            assertThat(updated.getAvailableQty()).isEqualTo(99);
        }

        @Test
        void shouldFindAllInventories() {
            inventoryRepository.save(Inventory.builder().productId("prod-all-1").availableQty(10).build());
            inventoryRepository.save(Inventory.builder().productId("prod-all-2").availableQty(20).build());
            entityManager.flush();

            var all = inventoryRepository.findAll();

            assertThat(all).hasSizeGreaterThanOrEqualTo(2);
            assertThat(all).extracting(Inventory::getProductId)
                .contains("prod-all-1", "prod-all-2");
        }
    }

    @Nested
    @DisplayName("StockMovementRepository")
    class StockMovementRepoTests {

        @Test
        void shouldSaveAndRetrieveMovement() {
            var orderId = UUID.randomUUID();
            var movement = StockMovement.builder()
                .productId("prod-mv-1")
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-5)
                .orderId(orderId)
                .build();

            var saved = stockMovementRepository.save(movement);
            entityManager.flush();
            entityManager.clear();

            var found = stockMovementRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getProductId()).isEqualTo("prod-mv-1");
            assertThat(found.get().getMovementType()).isEqualTo(StockMovement.MovementType.RESERVE);
            assertThat(found.get().getDelta()).isEqualTo(-5);
            assertThat(found.get().getOrderId()).isEqualTo(orderId);
            assertThat(found.get().getCreatedAt()).isNotNull();
        }

        @Test
        void shouldFindByProductIdOrderByCreatedAtDesc() {
            var m1 = StockMovement.builder()
                .productId("prod-ordered")
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-2)
                .build();
            stockMovementRepository.save(m1);
            entityManager.flush();

            var m2 = StockMovement.builder()
                .productId("prod-ordered")
                .movementType(StockMovement.MovementType.RELEASE)
                .delta(2)
                .build();
            stockMovementRepository.save(m2);
            entityManager.flush();

            var m3 = StockMovement.builder()
                .productId("prod-ordered")
                .movementType(StockMovement.MovementType.RESTOCK)
                .delta(10)
                .build();
            stockMovementRepository.save(m3);
            entityManager.flush();
            entityManager.clear();

            var movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc("prod-ordered");

            assertThat(movements).hasSize(3);
            for (int i = 0; i < movements.size() - 1; i++) {
                assertThat(movements.get(i).getCreatedAt())
                    .isAfterOrEqualTo(movements.get(i + 1).getCreatedAt());
            }
        }

        @Test
        void shouldReturnEmptyListForUnknownProductId() {
            var movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc("unknown-prod");

            assertThat(movements).isEmpty();
        }

        @Test
        void shouldFindByOrderIdAndMovementType() {
            var orderId = UUID.randomUUID();
            stockMovementRepository.save(StockMovement.builder()
                .productId("prod-filter-1")
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-3)
                .orderId(orderId)
                .build());
            stockMovementRepository.save(StockMovement.builder()
                .productId("prod-filter-2")
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-5)
                .orderId(orderId)
                .build());
            stockMovementRepository.save(StockMovement.builder()
                .productId("prod-filter-1")
                .movementType(StockMovement.MovementType.RELEASE)
                .delta(3)
                .orderId(orderId)
                .build());
            var differentOrderId = UUID.randomUUID();
            stockMovementRepository.save(StockMovement.builder()
                .productId("prod-filter-3")
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-1)
                .orderId(differentOrderId)
                .build());
            entityManager.flush();
            entityManager.clear();

            var reservations = stockMovementRepository
                .findByOrderIdAndMovementType(orderId, StockMovement.MovementType.RESERVE);

            assertThat(reservations).hasSize(2);
            assertThat(reservations).allMatch(m -> m.getOrderId().equals(orderId));
            assertThat(reservations).allMatch(m -> m.getMovementType() == StockMovement.MovementType.RESERVE);
            assertThat(reservations).extracting(StockMovement::getProductId)
                .containsExactlyInAnyOrder("prod-filter-1", "prod-filter-2");
        }

        @Test
        void shouldReturnEmptyWhenNoMatchingOrderIdAndType() {
            var orderId = UUID.randomUUID();

            var result = stockMovementRepository
                .findByOrderIdAndMovementType(orderId, StockMovement.MovementType.RESERVE);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldFilterByMovementTypeCorrectly() {
            var orderId = UUID.randomUUID();
            stockMovementRepository.save(StockMovement.builder()
                .productId("prod-type")
                .movementType(StockMovement.MovementType.RESERVE)
                .delta(-3)
                .orderId(orderId)
                .build());
            stockMovementRepository.save(StockMovement.builder()
                .productId("prod-type")
                .movementType(StockMovement.MovementType.RELEASE)
                .delta(3)
                .orderId(orderId)
                .build());
            entityManager.flush();
            entityManager.clear();

            var releases = stockMovementRepository
                .findByOrderIdAndMovementType(orderId, StockMovement.MovementType.RELEASE);

            assertThat(releases).hasSize(1);
            assertThat(releases.getFirst().getMovementType()).isEqualTo(StockMovement.MovementType.RELEASE);
        }
    }
}
