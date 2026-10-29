package com.pharmsaas.backend.modules.supplierpayment.controller;

import com.pharmsaas.backend.modules.supplierpayment.dto.SupplierPaymentRequest;
import com.pharmsaas.backend.modules.supplierpayment.dto.SupplierPaymentResponse;
import com.pharmsaas.backend.modules.supplierpayment.service.SupplierPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/supplier-payments")
public class SupplierPaymentController {

    private final SupplierPaymentService service;

    public SupplierPaymentController(SupplierPaymentService service) {
        this.service = service;
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Please login first");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }
    }

    @GetMapping
    public List<SupplierPaymentResponse> list(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long supplierId,
            Authentication authentication) {
        return service.list(tenantId, currentUserId(authentication), supplierId);
    }

    @GetMapping("/{paymentId}")
    public SupplierPaymentResponse get(
            @PathVariable Long tenantId,
            @PathVariable Long paymentId,
            Authentication authentication) {
        return service.get(tenantId, currentUserId(authentication), paymentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierPaymentResponse create(
            @PathVariable Long tenantId,
            @RequestBody SupplierPaymentRequest request,
            Authentication authentication) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @PatchMapping("/{paymentId}/cancel")
    public SupplierPaymentResponse cancel(
            @PathVariable Long tenantId,
            @PathVariable Long paymentId,
            Authentication authentication) {
        return service.cancel(tenantId, currentUserId(authentication), paymentId);
    }
}
