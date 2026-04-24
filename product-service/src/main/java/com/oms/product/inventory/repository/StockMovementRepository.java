package com.oms.product.inventory.repository;

import com.oms.product.inventory.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(String productId);
    List<StockMovement> findByOrderIdAndMovementType(UUID orderId, StockMovement.MovementType movementType);
}
