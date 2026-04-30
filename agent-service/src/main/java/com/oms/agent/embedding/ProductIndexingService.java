package com.oms.agent.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.agent.client.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexingService {

    private final VectorStore vectorStore;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void indexAllProducts() {
        log.info("Starting full product index into PgVector store");
        try {
            List<Document> allDocs = new ArrayList<>();
            int page = 0;
            int pageSize = 50;
            while (true) {
                List<Map<String, Object>> products = productClient.getAllProducts(page, pageSize);
                if (products.isEmpty()) break;
                products.stream().map(this::toDocument).forEach(allDocs::add);
                if (products.size() < pageSize) break;
                page++;
            }
            if (!allDocs.isEmpty()) {
                vectorStore.add(allDocs);
                log.info("Indexed {} products into PgVector store", allDocs.size());
            } else {
                log.warn("No products found to index — semantic search will fall back to keyword search until products are available");
            }
        } catch (Exception e) {
            log.warn("Product indexing failed on startup — semantic search will fall back to keyword search: {}", e.getMessage());
        }
    }

    public void indexProduct(String productId) {
        try {
            String json = productClient.getProductDetails(productId);
            @SuppressWarnings("unchecked")
            Map<String, Object> product = objectMapper.readValue(json, Map.class);
            vectorStore.delete(List.of(productId));
            vectorStore.add(List.of(toDocument(product)));
            log.info("Re-indexed product {}", productId);
        } catch (Exception e) {
            log.warn("Failed to re-index product {}: {}", productId, e.getMessage());
        }
    }

    public String semanticSearch(String query, Double minPrice, Double maxPrice, int topK) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).build());

            List<Map<String, Object>> results = docs.stream()
                    .map(Document::getMetadata)
                    .filter(meta -> inPriceRange(meta, minPrice, maxPrice))
                    .toList();

            if (docs.isEmpty()) {
                log.info("Vector store empty for query '{}', falling back to keyword search", query);
                return productClient.searchProducts(query, minPrice, maxPrice);
            }

            return objectMapper.writeValueAsString(
                    Map.of("results", results, "total", results.size(), "query", query));
        } catch (Exception e) {
            log.warn("Semantic search failed for '{}', falling back to keyword search: {}", query, e.getMessage());
            return productClient.searchProducts(query, minPrice, maxPrice);
        }
    }

    private boolean inPriceRange(Map<String, Object> meta, Double minPrice, Double maxPrice) {
        if (minPrice == null && maxPrice == null) return true;
        Object raw = meta.get("price");
        if (raw == null) return true;
        try {
            double price = Double.parseDouble(raw.toString());
            if (minPrice != null && price < minPrice) return false;
            if (maxPrice != null && price > maxPrice) return false;
            return true;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private Document toDocument(Map<String, Object> product) {
        String productId = String.valueOf(product.get("id"));
        String name     = String.valueOf(product.getOrDefault("name", ""));
        String desc     = String.valueOf(product.getOrDefault("description", ""));
        String category = String.valueOf(product.getOrDefault("category", ""));

        String text = "Name: " + name + "\nCategory: " + category + "\nDescription: " + desc;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", productId);
        metadata.put("name",      name);
        metadata.put("category",  category);
        metadata.put("price",     product.getOrDefault("price", "0"));
        metadata.put("stockQty",  product.getOrDefault("stockQty", 0));
        metadata.put("imageUrl",  product.getOrDefault("imageUrl", ""));
        metadata.put("active",    product.getOrDefault("active", true));

        return new Document(productId, text, metadata);
    }
}
