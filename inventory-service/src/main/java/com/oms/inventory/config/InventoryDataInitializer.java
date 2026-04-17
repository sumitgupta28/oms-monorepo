package com.oms.inventory.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oms.inventory.domain.Inventory;
import com.oms.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryDataInitializer implements ApplicationRunner {

    private final InventoryRepository inventoryRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8084}")
    private String productServiceUrl;

    private static final Random RANDOM = new Random(42);

    @Override
    public void run(ApplicationArguments args) {
        if (inventoryRepository.count() >= 100) {
            log.info("Inventory already seeded — skipping initialization");
            return;
        }

        List<String> productIds = fetchAllProductIds();
        if (productIds.isEmpty()) {
            log.warn("No products found in product-service — skipping inventory seeding");
            return;
        }

        List<Inventory> records = productIds.stream()
                .filter(id -> !inventoryRepository.existsById(id))
                .map(id -> Inventory.builder()
                        .productId(id)
                        .availableQty(10 + RANDOM.nextInt(491))
                        .reservedQty(0)
                        .build())
                .toList();

        inventoryRepository.saveAll(records);
        log.info("Seeded {} inventory records", records.size());
    }

    private List<String> fetchAllProductIds() {
        List<String> ids = new ArrayList<>();
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            try {
                String url = productServiceUrl + "/products?page=" + page + "&size=50";
                var response = restTemplate.exchange(
                        url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<PageResponse<ProductSummary>>() {});
                PageResponse<ProductSummary> body = response.getBody();
                if (body == null || body.content().isEmpty()) break;
                body.content().forEach(p -> ids.add(p.id()));
                hasMore = !body.last();
                page++;
            } catch (Exception e) {
                log.warn("Could not fetch products from product-service: {}", e.getMessage());
                break;
            }
        }
        return ids;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductSummary(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PageResponse<T>(List<T> content, boolean last) {}
}
