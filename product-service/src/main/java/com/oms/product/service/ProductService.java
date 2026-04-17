package com.oms.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.events.ProductUpdatedEvent;
import com.oms.product.domain.Product;
import com.oms.product.exception.ProductNotFoundException;
import com.oms.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class ProductService {
    private final ProductRepository productRepo;
    private final KafkaTemplate<String,String> kafka;
    private final ObjectMapper objectMapper;

    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepo.findByActiveTrue(pageable).map(ProductResponse::from);
    }

    public ProductResponse getById(String id) {
        return ProductResponse.from(findProduct(id));
    }

    public ProductResponse create(CreateProductRequest req) {
        Product p = Product.builder().name(req.name()).description(req.description())
            .category(req.category()).price(req.price()).stockQty(req.stockQty())
            .imageUrl(req.imageUrl()).build();
        Product saved = productRepo.save(p);
        log.info("Product created: {}", saved.getId());
        return ProductResponse.from(saved);
    }

    public ProductResponse update(String id, UpdateProductRequest req) {
        Product p = findProduct(id);
        p.setName(req.name()); p.setDescription(req.description());
        p.setCategory(req.category()); p.setPrice(req.price());
        p.setStockQty(req.stockQty()); p.setImageUrl(req.imageUrl());
        p.setUpdatedAt(Instant.now());
        Product saved = productRepo.save(p);
        try {
            kafka.send("oms.products.updated",
                objectMapper.writeValueAsString(
                    new ProductUpdatedEvent(id, req.name(), req.description())));
        } catch (Exception e) {
            log.warn("Failed to publish product.updated event for {}: {}", id, e.getMessage(), e);
        }
        return ProductResponse.from(saved);
    }

    public void delete(String id) {
        Product p = findProduct(id);
        p.setActive(false);
        productRepo.save(p);
    }

    public SearchResponse search(String query) {
        List<ProductResponse> results = productRepo.searchByKeyword(query).stream()
            .map(ProductResponse::from).toList();
        return new SearchResponse(results, results.size(), query);
    }

    private Product findProduct(String id) {
        return productRepo.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
    }
}
