package com.pharmsaas.backend.modules.inventory.controller;

import com.pharmsaas.backend.modules.inventory.dto.InventoryItemRequest;
import com.pharmsaas.backend.modules.inventory.dto.InventoryItemResponse;
import com.pharmsaas.backend.modules.inventory.service.TenantInventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/inventory/medicines")
public class TenantInventoryController {

    private final TenantInventoryService inventoryService;

    public TenantInventoryController(TenantInventoryService inventoryService) {
        this.inventoryService = inventoryService;
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
    public List<InventoryItemResponse> list(
            @PathVariable Long tenantId, Authentication authentication) {
        return inventoryService.list(tenantId, currentUserId(authentication));
    }

    @GetMapping("/{itemId}")
    public InventoryItemResponse get(
            @PathVariable Long tenantId,
            @PathVariable Long itemId,
            Authentication authentication) {
        return inventoryService.get(
            tenantId, currentUserId(authentication), itemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(
            @PathVariable Long tenantId,
            @RequestBody InventoryItemRequest request,
            Authentication authentication) {
        return inventoryService.create(
            tenantId, currentUserId(authentication), request);
    }

    @PutMapping("/{itemId}")
    public InventoryItemResponse update(
            @PathVariable Long tenantId,
            @PathVariable Long itemId,
            @RequestBody InventoryItemRequest request,
            Authentication authentication) {
        return inventoryService.update(
            tenantId, currentUserId(authentication), itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long tenantId,
            @PathVariable Long itemId,
            Authentication authentication) {
        inventoryService.delete(
            tenantId, currentUserId(authentication), itemId);
    }
}
