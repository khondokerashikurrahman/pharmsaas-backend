package com.pharmsaas.backend.modules.grn.controller;

import com.pharmsaas.backend.modules.grn.dto.GrnCreateRequest;
import com.pharmsaas.backend.modules.grn.dto.GrnResponse;
import com.pharmsaas.backend.modules.grn.service.GrnService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/grn")
public class GrnController {

    private final GrnService grnService;

    public GrnController(GrnService grnService) {
        this.grnService = grnService;
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
    public GrnResponse create(
            @PathVariable Long tenantId,
            @RequestBody GrnCreateRequest request,
            Authentication authentication) {
        return grnService.create(tenantId, currentUserId(authentication), request);
    }

    @GetMapping
    public List<GrnResponse> list(
            @PathVariable Long tenantId,
            Authentication authentication) {
        return grnService.list(tenantId, currentUserId(authentication));
    }

    @GetMapping("/{grnId}")
    public GrnResponse get(
            @PathVariable Long tenantId,
            @PathVariable Long grnId,
            Authentication authentication) {
        return grnService.get(tenantId, currentUserId(authentication), grnId);
    }
}
