package com.pharmsaas.backend.modules.sale.controller;

import com.pharmsaas.backend.modules.sale.dto.SaleCreateRequest;
import com.pharmsaas.backend.modules.sale.dto.SaleResponse;
import com.pharmsaas.backend.modules.sale.dto.SaleReturnCreateRequest;
import com.pharmsaas.backend.modules.sale.dto.SaleReturnResponse;
import com.pharmsaas.backend.modules.sale.service.SaleReturnService;
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
    private final SaleReturnService saleReturnService;

    public SaleController(
        SaleService saleService,
        SaleReturnService saleReturnService
    ) {
        this.saleService = saleService;
        this.saleReturnService = saleReturnService;
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

    @PostMapping("/{saleId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleReturnResponse returnSale(
        @PathVariable Long tenantId,
        @PathVariable Long saleId,
        @RequestBody SaleReturnCreateRequest request,
        Authentication authentication
    ) {
        return saleReturnService.create(
            tenantId, saleId, currentUserId(authentication), request
        );
    }

    @GetMapping("/returns")
    public List<SaleReturnResponse> listReturns(
        @PathVariable Long tenantId,
        Authentication authentication
    ) {
        return saleReturnService.listReturns(tenantId, currentUserId(authentication));
    }

    @PostMapping("/returns/{returnId}/items/{returnItemId}/restock")
    public SaleReturnResponse restockReturnedItem(
        @PathVariable Long tenantId,
        @PathVariable Long returnId,
        @PathVariable Long returnItemId,
        Authentication authentication
    ) {
        return saleReturnService.restockReturnedItem(
            tenantId, returnId, returnItemId, currentUserId(authentication)
        );
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
