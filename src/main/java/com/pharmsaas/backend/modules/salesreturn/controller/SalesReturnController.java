package com.pharmsaas.backend.modules.salesreturn.controller;

import com.pharmsaas.backend.modules.salesreturn.dto.SaleReturnCreateRequest;
import com.pharmsaas.backend.modules.salesreturn.dto.SaleReturnResponse;
import com.pharmsaas.backend.modules.salesreturn.service.SalesReturnService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/sales")
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    public SalesReturnController(SalesReturnService salesReturnService) {
        this.salesReturnService = salesReturnService;
    }

    @PostMapping("/{saleId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleReturnResponse returnSale(
        @PathVariable Long tenantId,
        @PathVariable Long saleId,
        @RequestBody SaleReturnCreateRequest request,
        Authentication authentication
    ) {
        return salesReturnService.create(
            tenantId, saleId, currentUserId(authentication), request
        );
    }

    @GetMapping("/returns")
    public List<SaleReturnResponse> listReturns(
        @PathVariable Long tenantId,
        Authentication authentication
    ) {
        return salesReturnService.listReturns(tenantId, currentUserId(authentication));
    }

    @PostMapping("/returns/{returnId}/items/{returnItemId}/restock")
    public SaleReturnResponse restockReturnedItem(
        @PathVariable Long tenantId,
        @PathVariable Long returnId,
        @PathVariable Long returnItemId,
        Authentication authentication
    ) {
        return salesReturnService.restockReturnedItem(
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
