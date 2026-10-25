
package com.pharmsaas.backend.modules.membership.controller;

import com.pharmsaas.backend.modules.membership.dto.MembershipRequest;
import com.pharmsaas.backend.modules.membership.dto.MembershipResponse;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.service.TenantMembershipService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants/{tenantId}/memberships")
public class TenantMembershipController {

    private final TenantMembershipService membershipService;

    public TenantMembershipController(
            TenantMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user is required."
            );
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user ID."
            );
        }
    }

    private void checkTenantAdmin(Long tenantId, Long currentUserId) {
        boolean isOwner = membershipService.hasRole(
                tenantId, currentUserId, MembershipRole.TENANT_OWNER
        );

        boolean isAdmin = membershipService.hasRole(
                tenantId, currentUserId, MembershipRole.PHARMACY_ADMIN
        );

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to manage this tenant."
            );
        }
    }

    // Add an existing user to this tenant.
    @PostMapping
    public ResponseEntity<MembershipResponse> addMember(
            @PathVariable Long tenantId,
            @RequestBody MembershipRequest request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        if (request == null
                || request.getUserId() == null
                || request.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID and role are required."
            );
        }

        TenantMembership membership = membershipService.addMembership(
                tenantId,
                currentUserId,
                request.getUserId(),
                request.getRole()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(membership));
    }

    // Get all tenant memberships, including inactive members.
    @GetMapping
    public ResponseEntity<List<MembershipResponse>> getMembers(
            @PathVariable Long tenantId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        checkTenantAdmin(tenantId, currentUserId);

        List<MembershipResponse> members =
                membershipService.getTenantMembers(tenantId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(members);
    }

    // Change a member's role.
    @PutMapping("/{userId}/role")
    public ResponseEntity<MembershipResponse> changeMemberRole(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @RequestBody MembershipRequest request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        if (request == null || request.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A role is required."
            );
        }

        TenantMembership updated = membershipService.changeMembershipRole(
                tenantId,
                currentUserId,
                userId,
                request.getRole()
        );

        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        membershipService.removeTenantMember(
                tenantId,
                currentUserId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    // Activate or deactivate a member.
    @PutMapping("/{userId}/status")
    public ResponseEntity<MembershipResponse> changeMemberStatus(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        if (request == null || request.get("active") == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The active field is required."
            );
        }

        TenantMembership updated =
                membershipService.changeTenantMemberStatus(
                        tenantId,
                        currentUserId,
                        userId,
                        request.get("active")
                );

        return ResponseEntity.ok(toResponse(updated));
    }

    // Convert entity to API response.
    private MembershipResponse toResponse(
            TenantMembership membership) {

        return new MembershipResponse(
                membership.getId(),
                membership.getTenant().getId(),
                membership.getUser().getId(),
                membership.getUser().getFullName(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.isActive()
        );
    }
}
