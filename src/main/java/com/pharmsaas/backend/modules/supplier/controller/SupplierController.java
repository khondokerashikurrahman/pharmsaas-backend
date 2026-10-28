package com.pharmsaas.backend.modules.supplier.controller;

import com.pharmsaas.backend.modules.supplier.dto.SupplierRequest;
import com.pharmsaas.backend.modules.supplier.dto.SupplierResponse;
import com.pharmsaas.backend.modules.supplier.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/suppliers")
public class SupplierController {
    private final SupplierService service;

    public SupplierController(SupplierService service) {
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
    public List<SupplierResponse> list(
            @PathVariable Long tenantId, Authentication authentication) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @GetMapping("/{supplierId}")
    public SupplierResponse get(
            @PathVariable Long tenantId,
            @PathVariable Long supplierId,
            Authentication authentication) {
        return service.get(
            tenantId, currentUserId(authentication), supplierId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(
            @PathVariable Long tenantId,
            @RequestBody SupplierRequest request,
            Authentication authentication) {
        return service.create(
            tenantId, currentUserId(authentication), request);
    }

    @PutMapping("/{supplierId}")
    public SupplierResponse update(
            @PathVariable Long tenantId,
            @PathVariable Long supplierId,
            @RequestBody SupplierRequest request,
            Authentication authentication) {
        return service.update(
            tenantId, currentUserId(authentication), supplierId, request);
    }

    @PatchMapping("/{supplierId}/deactivate")
    public SupplierResponse deactivate(
            @PathVariable Long tenantId,
            @PathVariable Long supplierId,
            Authentication authentication) {
        return service.deactivate(
            tenantId, currentUserId(authentication), supplierId);
    }
}
