package com.oms.product.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection="products")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id private String id;
    @Indexed private String name;
    private String description;
    @Indexed private String category;
    private BigDecimal price;
    private int stockQty;
    private String imageUrl;
    @Builder.Default private boolean active = true;
    @Builder.Default private Instant createdAt = Instant.now();
    private Instant updatedAt;
}
