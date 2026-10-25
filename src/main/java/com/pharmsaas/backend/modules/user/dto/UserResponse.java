package com.pharmsaas.backend.modules.user.dto;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        boolean enabled,
        String systemRole
) {
}