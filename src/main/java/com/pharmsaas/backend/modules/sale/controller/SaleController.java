package com.pharmsaas.backend.modules.sale.controller;

import com.pharmsaas.backend.modules.sale.dto.SaleCreateRequest;
import com.pharmsaas.backend.modules.sale.dto.SaleResponse;
import com.pharmsaas.backend.modules.sale.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(
        @PathVariable Long tenantId,
        @Valid @RequestBody SaleCreateRequest request,
        Authentication authentication
    ) {
        return saleService.create(tenantId, currentUserId(authentication), request);
    }

    @GetMapping
    public List<SaleResponse> list(
        @PathVariable Long tenantId,
        Authentication authentication
    ) {
        return saleService.list(tenantId, currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Authentication required"
            );
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user"
            );
        }
    }
}
