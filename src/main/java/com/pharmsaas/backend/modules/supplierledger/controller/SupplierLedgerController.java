package com.pharmsaas.backend.modules.supplierledger.controller;

import com.pharmsaas.backend.modules.supplierledger.dto.SupplierLedgerResponse;
import com.pharmsaas.backend.modules.supplierledger.service.SupplierLedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tenants/{tenantId}/suppliers/{supplierId}/ledger")
public class SupplierLedgerController {

    private final SupplierLedgerService service;

    public SupplierLedgerController(SupplierLedgerService service) {
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
    public SupplierLedgerResponse getLedger(
            @PathVariable Long tenantId,
            @PathVariable Long supplierId,
            Authentication authentication) {
        return service.getLedger(
            tenantId, currentUserId(authentication), supplierId);
    }
}
