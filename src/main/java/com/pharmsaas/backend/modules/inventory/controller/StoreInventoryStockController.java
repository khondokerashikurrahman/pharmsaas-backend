package com.pharmsaas.backend.modules.inventory.controller;

import com.pharmsaas.backend.modules.inventory.dto.StoreInventoryStockResponse;
import com.pharmsaas.backend.modules.inventory.dto.StoreStockAllocationRequest;
import com.pharmsaas.backend.modules.inventory.service.StoreInventoryStockService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/stores/{storeId}/inventory")
public class StoreInventoryStockController {

    private final StoreInventoryStockService service;

    public StoreInventoryStockController(
            StoreInventoryStockService service) {
        this.service = service;
    }

    private Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (Exception exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }
    }

    @GetMapping
    public List<StoreInventoryStockResponse> list(
            @PathVariable Long tenantId,
            @PathVariable Long storeId,
            Authentication authentication) {
        return service.list(
            tenantId, storeId, currentUserId(authentication));
    }

    @PostMapping("/allocate")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreInventoryStockResponse allocate(
            @PathVariable Long tenantId,
            @PathVariable Long storeId,
            @RequestBody StoreStockAllocationRequest request,
            Authentication authentication) {
        return service.allocate(
            tenantId, storeId, currentUserId(authentication), request);
    }
}
