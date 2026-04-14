package com.oms.product.service;

import com.oms.product.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor @Slf4j
public class EmbeddingService {
    private final VectorStore vectorStore;

    public void embedProduct(Product product) {
        String content = product.getName() + " " + product.getDescription()
            + " category:" + product.getCategory()
            + " price:" + product.getPrice();
        Document doc = new Document(content, Map.of(
            "productId", product.getId(),
            "name",      product.getName(),
            "category",  product.getCategory(),
            "price",     product.getPrice().toString()
        ));
        vectorStore.add(List.of(doc));
        log.info("Embedded product: {}", product.getId());
    }

    public void deleteEmbedding(String productId) {
        vectorStore.delete(List.of(productId));
    }

    public List<String> semanticSearch(String query, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build())
            .stream()
            .map(doc -> (String) doc.getMetadata().get("productId"))
            .toList();
    }
}
