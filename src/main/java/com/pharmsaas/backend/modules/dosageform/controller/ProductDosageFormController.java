package com.pharmsaas.backend.modules.dosageform.controller;

import com.pharmsaas.backend.modules.dosageform.dto.DosageFormRequest;
import com.pharmsaas.backend.modules.dosageform.dto.DosageFormResponse;
import com.pharmsaas.backend.modules.dosageform.service.ProductDosageFormService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/dosage-forms")
public class ProductDosageFormController {
    private final ProductDosageFormService service;

    public ProductDosageFormController(ProductDosageFormService service) {
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
    public List<DosageFormResponse> list(
        @PathVariable Long tenantId,
        Authentication authentication
    ) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DosageFormResponse create(
        @PathVariable Long tenantId,
        @Valid @RequestBody DosageFormRequest request,
        Authentication authentication
    ) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @PutMapping("/{dosageFormId}")
    public DosageFormResponse update(
        @PathVariable Long tenantId,
        @PathVariable Long dosageFormId,
        @Valid @RequestBody DosageFormRequest request,
        Authentication authentication
    ) {
        return service.update(
            tenantId, currentUserId(authentication), dosageFormId, request
        );
    }

    @DeleteMapping("/{dosageFormId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
        @PathVariable Long tenantId,
        @PathVariable Long dosageFormId,
        Authentication authentication
    ) {
        service.archive(tenantId, currentUserId(authentication), dosageFormId);
    }
}
