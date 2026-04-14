package com.oms.inventory.controller;

import com.oms.inventory.service.AdjustStockRequest;
import com.oms.inventory.service.InventoryResponse;
import com.oms.inventory.service.InventoryService;
import com.oms.inventory.service.SetStockRequest;
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
    public List<InventoryResponse> getAllStock() { return inventoryService.getAllStock(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse setStock(@RequestBody SetStockRequest req) {
        return inventoryService.setStock(req.productId(), req.quantity());
    }

    @PatchMapping("/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse adjust(@PathVariable String productId, @RequestBody AdjustStockRequest req) {
        return inventoryService.adjustStock(productId, req.delta());
    }
}
