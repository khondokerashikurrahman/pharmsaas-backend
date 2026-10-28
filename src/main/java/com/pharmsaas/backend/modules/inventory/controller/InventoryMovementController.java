package com.pharmsaas.backend.modules.inventory.controller;

import com.pharmsaas.backend.modules.inventory.dto.InventoryMovementResponse;
import com.pharmsaas.backend.modules.inventory.service.InventoryMovementService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/inventory/movements")
public class InventoryMovementController {

    private final InventoryMovementService movementService;

    public InventoryMovementController(InventoryMovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public List<InventoryMovementResponse> list(
        @PathVariable Long tenantId,
        Authentication authentication
    ) {
        return movementService.list(tenantId, currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Authentication required"
            );
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user"
            );
        }
    }
}
