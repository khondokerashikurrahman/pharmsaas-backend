package com.pharmsaas.backend.modules.company.controller;

import com.pharmsaas.backend.modules.company.dto.CompanyRequest;
import com.pharmsaas.backend.modules.company.dto.CompanyResponse;
import com.pharmsaas.backend.modules.company.service.ManufacturerCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/companies")
public class ManufacturerCompanyController {
    private final ManufacturerCompanyService service;

    public ManufacturerCompanyController(ManufacturerCompanyService service) {
        this.service = service;
    }

    private Long currentUserId(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                throw new NumberFormatException();
            }
            return Long.valueOf(authentication.getName());
        } catch (Exception exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Authentication required"
            );
        }
    }

    @GetMapping
    public List<CompanyResponse> list(
        @PathVariable Long tenantId, Authentication authentication
    ) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @GetMapping("/{companyId}")
    public CompanyResponse get(
        @PathVariable Long tenantId,
        @PathVariable Long companyId,
        Authentication authentication
    ) {
        return service.get(tenantId, currentUserId(authentication), companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(
        @PathVariable Long tenantId,
        @Valid @RequestBody CompanyRequest request,
        Authentication authentication
    ) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @PutMapping("/{companyId}")
    public CompanyResponse update(
        @PathVariable Long tenantId,
        @PathVariable Long companyId,
        @Valid @RequestBody CompanyRequest request,
        Authentication authentication
    ) {
        return service.update(
            tenantId, currentUserId(authentication), companyId, request
        );
    }

    @PatchMapping("/{companyId}/deactivate")
    public CompanyResponse deactivate(
        @PathVariable Long tenantId,
        @PathVariable Long companyId,
        Authentication authentication
    ) {
        return service.deactivate(
            tenantId, currentUserId(authentication), companyId
        );
    }
}
