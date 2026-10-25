
package com.pharmsaas.backend.modules.membership.dto;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;

public class MembershipResponse {

    private Long membershipId;
    private Long tenantId;
    private Long userId;
    private String fullName;
    private String email;
    private MembershipRole role;
    private boolean active;

    public MembershipResponse(
            Long membershipId,
            Long tenantId,
            Long userId,
            String fullName,
            String email,
            MembershipRole role,
            boolean active) {

        this.membershipId = membershipId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public MembershipRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
