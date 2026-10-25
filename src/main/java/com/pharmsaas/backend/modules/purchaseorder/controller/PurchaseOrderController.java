package com.pharmsaas.backend.modules.purchaseorder.controller;

import com.pharmsaas.backend.modules.purchaseorder.dto.PurchaseOrderRequest;
import com.pharmsaas.backend.modules.purchaseorder.dto.PurchaseOrderResponse;
import com.pharmsaas.backend.modules.purchaseorder.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) {
        this.service = service;
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
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(
            @PathVariable Long tenantId,
            @RequestBody PurchaseOrderRequest request,
            Authentication authentication) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @GetMapping
    public List<PurchaseOrderResponse> list(
            @PathVariable Long tenantId,
            Authentication authentication) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @GetMapping("/{orderId}")
    public PurchaseOrderResponse get(
            @PathVariable Long tenantId,
            @PathVariable Long orderId,
            Authentication authentication) {
        return service.get(tenantId, currentUserId(authentication), orderId);
    }

    @PutMapping("/{orderId}")
    public PurchaseOrderResponse update(
            @PathVariable Long tenantId,
            @PathVariable Long orderId,
            @RequestBody PurchaseOrderRequest request,
            Authentication authentication) {
        return service.update(
            tenantId, currentUserId(authentication), orderId, request);
    }

    @PatchMapping("/{orderId}/submit")
    public PurchaseOrderResponse submit(
            @PathVariable Long tenantId,
            @PathVariable Long orderId,
            Authentication authentication) {
        return service.submit(tenantId, currentUserId(authentication), orderId);
    }

    @PatchMapping("/{orderId}/cancel")
    public PurchaseOrderResponse cancel(
            @PathVariable Long tenantId,
            @PathVariable Long orderId,
            Authentication authentication) {
        return service.cancel(tenantId, currentUserId(authentication), orderId);
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long tenantId,
            @PathVariable Long orderId,
            Authentication authentication) {
        service.delete(tenantId, currentUserId(authentication), orderId);
    }
}
