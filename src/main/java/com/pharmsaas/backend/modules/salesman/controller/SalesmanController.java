package com.pharmsaas.backend.modules.salesman.controller;

import com.pharmsaas.backend.modules.salesman.dto.SalesmanRequest;
import com.pharmsaas.backend.modules.salesman.dto.SalesmanResponse;
import com.pharmsaas.backend.modules.salesman.service.SalesmanService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/salesmen")
public class SalesmanController {

    private final SalesmanService service;

    public SalesmanController(SalesmanService service) {
        this.service = service;
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Please login first");
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }
    }

    @GetMapping
    public List<SalesmanResponse> list(
            @PathVariable Long tenantId, Authentication authentication) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @GetMapping("/{salesmanId}")
    public SalesmanResponse get(
            @PathVariable Long tenantId,
            @PathVariable Long salesmanId,
            Authentication authentication) {
        return service.get(tenantId, currentUserId(authentication), salesmanId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesmanResponse create(
            @PathVariable Long tenantId,
            @RequestBody SalesmanRequest request,
            Authentication authentication) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @PutMapping("/{salesmanId}")
    public SalesmanResponse update(
            @PathVariable Long tenantId,
            @PathVariable Long salesmanId,
            @RequestBody SalesmanRequest request,
            Authentication authentication) {
        return service.update(
            tenantId, currentUserId(authentication), salesmanId, request);
    }

    @PatchMapping("/{salesmanId}/deactivate")
    public SalesmanResponse deactivate(
            @PathVariable Long tenantId,
            @PathVariable Long salesmanId,
            Authentication authentication) {
        return service.deactivate(
            tenantId, currentUserId(authentication), salesmanId);
    }
}
