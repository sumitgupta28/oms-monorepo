package com.oms.product.controller;

import com.oms.product.dto.ProductResponse;
import com.oms.product.dto.SearchResponse;
import com.oms.product.exception.ProductNotFoundException;
import com.oms.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProductGraphQLController {

    private final ProductService productService;

    @QueryMapping
    public SearchResponse searchProducts(
            @Argument String query,
            @Argument String minPrice,
            @Argument String maxPrice) {
        BigDecimal min = (minPrice != null && !minPrice.isBlank()) ? new BigDecimal(minPrice) : null;
        BigDecimal max = (maxPrice != null && !maxPrice.isBlank()) ? new BigDecimal(maxPrice) : null;
        return productService.search(query, min, max);
    }

    @QueryMapping
    public ProductResponse product(@Argument String id) {
        try {
            return productService.getById(id);
        } catch (ProductNotFoundException e) {
            return null;
        }
    }

    @QueryMapping
    public Map<String, Object> products(
            @Argument int page,
            @Argument int size,
            @Argument String sort) {
        Page<ProductResponse> result = productService.getAll(PageRequest.of(page, size, Sort.by(sort)));
        return Map.of(
            "content",       result.getContent(),
            "totalElements", (int) result.getTotalElements(),
            "totalPages",    result.getTotalPages(),
            "pageNumber",    result.getNumber(),
            "pageSize",      result.getSize()
        );
    }
}
