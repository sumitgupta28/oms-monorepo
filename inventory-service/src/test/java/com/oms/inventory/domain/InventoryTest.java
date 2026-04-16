package com.oms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inventory entity")
class InventoryTest {

    @Nested
    @DisplayName("builder defaults")
    class BuilderDefaults {

        @Test
        void shouldDefaultAvailableQtyToZero() {
            var inventory = Inventory.builder().productId("p1").build();

            assertThat(inventory.getAvailableQty()).isZero();
        }

        @Test
        void shouldDefaultReservedQtyToZero() {
            var inventory = Inventory.builder().productId("p1").build();

            assertThat(inventory.getReservedQty()).isZero();
        }

        @Test
        void shouldSetProductId() {
            var inventory = Inventory.builder().productId("prod-42").build();

            assertThat(inventory.getProductId()).isEqualTo("prod-42");
        }
    }

    @Nested
    @DisplayName("canReserve")
    class CanReserve {

        @ParameterizedTest(name = "available={0}, requested={1}, expected={2}")
        @CsvSource({
            "10, 5,  true",
            "10, 10, true",
            "10, 11, false",
            "0,  1,  false",
            "0,  0,  true",
            "1,  1,  true",
        })
        void shouldReturnCorrectResult(int available, int requested, boolean expected) {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(available)
                .build();

            assertThat(inventory.canReserve(requested)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        void shouldDecrementAvailableQtyByRequestedAmount() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(10)
                .reservedQty(0)
                .build();

            inventory.reserve(3);

            assertThat(inventory.getAvailableQty()).isEqualTo(7);
        }

        @Test
        void shouldIncrementReservedQtyByRequestedAmount() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(10)
                .reservedQty(2)
                .build();

            inventory.reserve(3);

            assertThat(inventory.getReservedQty()).isEqualTo(5);
        }

        @Test
        void shouldHandleReservingEntireAvailableStock() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(5)
                .reservedQty(0)
                .build();

            inventory.reserve(5);

            assertThat(inventory.getAvailableQty()).isZero();
            assertThat(inventory.getReservedQty()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        void shouldIncrementAvailableQty() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(5)
                .reservedQty(3)
                .build();

            inventory.release(2);

            assertThat(inventory.getAvailableQty()).isEqualTo(7);
        }

        @Test
        void shouldDecrementReservedQty() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(5)
                .reservedQty(3)
                .build();

            inventory.release(2);

            assertThat(inventory.getReservedQty()).isEqualTo(1);
        }

        @Test
        void shouldHandleReleasingAllReservedStock() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(0)
                .reservedQty(10)
                .build();

            inventory.release(10);

            assertThat(inventory.getAvailableQty()).isEqualTo(10);
            assertThat(inventory.getReservedQty()).isZero();
        }
    }

    @Nested
    @DisplayName("deduct")
    class Deduct {

        @Test
        void shouldDecrementReservedQtyOnly() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(5)
                .reservedQty(3)
                .build();

            inventory.deduct(2);

            assertThat(inventory.getReservedQty()).isEqualTo(1);
            assertThat(inventory.getAvailableQty()).isEqualTo(5);
        }

        @Test
        void shouldNotAffectAvailableQty() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(10)
                .reservedQty(5)
                .build();

            inventory.deduct(5);

            assertThat(inventory.getAvailableQty()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("addStock")
    class AddStock {

        @Test
        void shouldIncrementAvailableQtyOnly() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(5)
                .reservedQty(3)
                .build();

            inventory.addStock(10);

            assertThat(inventory.getAvailableQty()).isEqualTo(15);
            assertThat(inventory.getReservedQty()).isEqualTo(3);
        }

        @Test
        void shouldHandleAddingToZeroStock() {
            var inventory = Inventory.builder().productId("p1").build();

            inventory.addStock(7);

            assertThat(inventory.getAvailableQty()).isEqualTo(7);
        }

        @Test
        void shouldNotAffectReservedQty() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(0)
                .reservedQty(4)
                .build();

            inventory.addStock(10);

            assertThat(inventory.getReservedQty()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("combined operations")
    class CombinedOperations {

        @Test
        void shouldMaintainCorrectStateAfterReserveThenRelease() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(20)
                .reservedQty(0)
                .build();

            inventory.reserve(5);
            inventory.reserve(3);
            inventory.release(2);

            assertThat(inventory.getAvailableQty()).isEqualTo(14);
            assertThat(inventory.getReservedQty()).isEqualTo(6);
        }

        @Test
        void shouldMaintainCorrectStateAfterReserveThenDeduct() {
            var inventory = Inventory.builder()
                .productId("p1")
                .availableQty(20)
                .reservedQty(0)
                .build();

            inventory.reserve(8);
            inventory.deduct(8);

            assertThat(inventory.getAvailableQty()).isEqualTo(12);
            assertThat(inventory.getReservedQty()).isZero();
        }
    }
}
