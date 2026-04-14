package com.oms.product.service;

import java.util.List;

public record SearchResponse(List<ProductResponse> results, int total, String query) {
}
