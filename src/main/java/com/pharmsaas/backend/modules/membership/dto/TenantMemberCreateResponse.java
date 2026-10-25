package com.pharmsaas.backend.modules.membership.dto;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;

public record TenantMemberCreateResponse(
        Long membershipId,
        Long userId,
        Long tenantId,
        String fullName,
        String email,
        MembershipRole role,
        boolean active
) {}
