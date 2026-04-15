package com.oms.product.controller;

import com.oms.product.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/products") @RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> getAll(
        @RequestParam(defaultValue="0")  int page,
        @RequestParam(defaultValue="20") int size,
        @RequestParam(defaultValue="name") String sort) {
        return productService.getAll(PageRequest.of(page,size,Sort.by(sort)));
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable String id) { return productService.getById(id); }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) { return productService.semanticSearch(q); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.delete(id); return ResponseEntity.noContent().build();
    }
}
