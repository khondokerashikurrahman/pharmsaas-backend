package com.pharmsaas.backend.modules.membership.dto;


import com.pharmsaas.backend.modules.membership.entity.MembershipRole;

public class MembershipRequest {

    private Long userId;
    private MembershipRole role;

    public MembershipRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public MembershipRole getRole() {
        return role;
    }

    public void setRole(MembershipRole role) {
        this.role = role;
    }
}

