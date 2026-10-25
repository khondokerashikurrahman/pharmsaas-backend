package com.pharmsaas.backend.modules.inventory.controller;

import com.pharmsaas.backend.modules.inventory.dto.StockRequestCreate;
import com.pharmsaas.backend.modules.inventory.dto.StockRequestDecision;
import com.pharmsaas.backend.modules.inventory.dto.StockRequestResponse;
import com.pharmsaas.backend.modules.inventory.service.StockRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/inventory/stock-requests")
public class StockRequestController {

    private final StockRequestService service;

    public StockRequestController(StockRequestService service) {
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockRequestResponse create(
            @PathVariable Long tenantId,
            @RequestBody StockRequestCreate request,
            Authentication authentication) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @GetMapping
    public List<StockRequestResponse> list(
            @PathVariable Long tenantId,
            Authentication authentication) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @PostMapping("/{requestId}/approve")
    public StockRequestResponse approve(
            @PathVariable Long tenantId,
            @PathVariable Long requestId,
            @RequestBody(required = false) StockRequestDecision decision,
            Authentication authentication) {
        return service.approve(
            tenantId, requestId, currentUserId(authentication), decision);
    }

    @PostMapping("/{requestId}/reject")
    public StockRequestResponse reject(
            @PathVariable Long tenantId,
            @PathVariable Long requestId,
            @RequestBody(required = false) StockRequestDecision decision,
            Authentication authentication) {
        return service.reject(
            tenantId, requestId, currentUserId(authentication), decision);
    }
}
