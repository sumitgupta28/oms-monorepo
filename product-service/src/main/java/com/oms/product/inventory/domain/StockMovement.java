package com.oms.product.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "stock_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private String productId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MovementType movementType;
    @Column(nullable = false) private int delta;
    private UUID orderId;
    @CreationTimestamp private Instant createdAt;

    public enum MovementType { RESERVE, RELEASE, DEDUCT, RESTOCK }
}
