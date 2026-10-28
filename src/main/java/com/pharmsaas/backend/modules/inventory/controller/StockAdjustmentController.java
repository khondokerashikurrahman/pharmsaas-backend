package com.pharmsaas.backend.modules.inventory.controller;

import com.pharmsaas.backend.modules.inventory.dto.StockAdjustmentRequest;
import com.pharmsaas.backend.modules.inventory.dto.StockAdjustmentResponse;
import com.pharmsaas.backend.modules.inventory.service.StockAdjustmentService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/{tenantId}/inventory/adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService adjustmentService;

    public StockAdjustmentController(
            StockAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockAdjustmentResponse adjust(
            @PathVariable Long tenantId,
            @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {

        Long userId;

        try {
            userId = Long.valueOf(authentication.getName());
        } catch (Exception exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }

        return adjustmentService.adjust(tenantId, userId, request);
    }
}
