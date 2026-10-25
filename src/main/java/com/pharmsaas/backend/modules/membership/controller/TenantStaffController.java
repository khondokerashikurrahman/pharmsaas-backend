package com.pharmsaas.backend.modules.membership.controller;

import com.pharmsaas.backend.modules.membership.dto.TenantMemberCreateRequest;
import com.pharmsaas.backend.modules.membership.dto.TenantMemberCreateResponse;
import com.pharmsaas.backend.modules.membership.service.TenantStaffService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tenants/{tenantId}/memberships")
public class TenantStaffController {

    private final TenantStaffService tenantStaffService;

    public TenantStaffController(TenantStaffService tenantStaffService) {
        this.tenantStaffService = tenantStaffService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantMemberCreateResponse createMember(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantMemberCreateRequest request,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first."
            );
        }

        Long actorUserId;

        try {
            actorUserId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user."
            );
        }

        return tenantStaffService.createMember(
                tenantId,
                actorUserId,
                request
        );
    }
}
