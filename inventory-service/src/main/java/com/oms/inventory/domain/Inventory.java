package com.oms.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity @Table(name="inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {
    @Id private String productId;
    @Column(nullable=false) @Builder.Default private int availableQty = 0;
    @Column(nullable=false) @Builder.Default private int reservedQty = 0;
    @UpdateTimestamp private Instant updatedAt;

    public boolean canReserve(int qty) { return availableQty >= qty; }
    public void reserve(int qty)  { availableQty -= qty; reservedQty += qty; }
    public void release(int qty)  { reservedQty -= qty; availableQty += qty; }
    public void deduct(int qty)   { reservedQty -= qty; }
    public void addStock(int qty) { availableQty += qty; }
}
