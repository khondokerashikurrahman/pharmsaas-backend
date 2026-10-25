package com.pharmsaas.backend.modules.inventory.controller;

import com.pharmsaas.backend.modules.inventory.dto.InventoryBatchResponse;
import com.pharmsaas.backend.modules.inventory.dto.InventoryBatchCreateRequest;
import com.pharmsaas.backend.modules.inventory.dto.InventoryBatchCreateRequest;
import com.pharmsaas.backend.modules.inventory.service.InventoryBatchService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/inventory/batches")
public class InventoryBatchController {

    private final InventoryBatchService batchService;

    public InventoryBatchController(InventoryBatchService batchService) {
        this.batchService = batchService;
    }

    private Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }
    }

    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public InventoryBatchResponse create(
            @PathVariable Long tenantId,
            @RequestBody InventoryBatchCreateRequest request,
            Authentication authentication) {
        return batchService.create(
            tenantId, currentUserId(authentication), request);
    }

    @GetMapping
    public List<InventoryBatchResponse> list(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long itemId,
            Authentication authentication) {
        return batchService.list(
            tenantId, currentUserId(authentication), itemId);
    }

    @GetMapping("/expired")
    public List<InventoryBatchResponse> expired(
            @PathVariable Long tenantId,
            Authentication authentication) {
        return batchService.expired(
            tenantId, currentUserId(authentication));
    }

    @GetMapping("/expiring")
    public List<InventoryBatchResponse> expiring(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "90") int days,
            Authentication authentication) {
        return batchService.expiring(
            tenantId, currentUserId(authentication), days);
    }
}
