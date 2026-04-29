package com.oms.product.inventory.controller;

import com.oms.product.inventory.dto.AdjustStockRequest;
import com.oms.product.inventory.dto.InventoryResponse;
import com.oms.product.inventory.dto.SetStockRequest;
import com.oms.product.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/inventory") @RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public InventoryResponse getStock(@PathVariable String productId) {
        return inventoryService.getStock(productId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<InventoryResponse> getAllStock() {
        return inventoryService.getAllStock();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse setStock(@Valid @RequestBody SetStockRequest req) {
        return inventoryService.setStock(req.productId(), req.quantity());
    }

    @PatchMapping("/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse adjust(@PathVariable String productId, @Valid @RequestBody AdjustStockRequest req) {
        return inventoryService.adjustStock(productId, req.delta());
    }
}
