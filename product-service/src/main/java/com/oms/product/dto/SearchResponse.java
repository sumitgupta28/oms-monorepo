package com.oms.product.dto;

import java.util.List;

public record SearchResponse(List<ProductResponse> results, int total, String query) {
}
