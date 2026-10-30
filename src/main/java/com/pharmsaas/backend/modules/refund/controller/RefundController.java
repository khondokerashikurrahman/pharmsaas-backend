package com.pharmsaas.backend.modules.refund.controller;

import com.pharmsaas.backend.modules.refund.dto.*;
import com.pharmsaas.backend.modules.refund.service.RefundService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/refunds")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/returns/{returnId}")
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse create(
        @PathVariable Long tenantId,
        @PathVariable Long returnId,
        @RequestBody RefundCreateRequest request,
        Authentication authentication
    ) {
        return refundService.create(tenantId, returnId, userId(authentication), request);
    }

    @GetMapping
    public List<RefundResponse> list(
        @PathVariable Long tenantId,
        Authentication authentication
    ) {
        return refundService.list(tenantId, userId(authentication));
    }

    @PostMapping("/{refundId}/complete")
    public RefundResponse complete(
        @PathVariable Long tenantId,
        @PathVariable Long refundId,
        @RequestBody(required = false) RefundProcessRequest request,
        Authentication authentication
    ) {
        return refundService.complete(tenantId, refundId, userId(authentication), request);
    }

    @PostMapping("/{refundId}/fail")
    public RefundResponse fail(
        @PathVariable Long tenantId,
        @PathVariable Long refundId,
        @RequestBody(required = false) RefundProcessRequest request,
        Authentication authentication
    ) {
        return refundService.fail(tenantId, refundId, userId(authentication), request);
    }

    private Long userId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }
    }
}
