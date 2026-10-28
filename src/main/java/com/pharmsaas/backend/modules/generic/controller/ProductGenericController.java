package com.pharmsaas.backend.modules.generic.controller;

import com.pharmsaas.backend.modules.generic.dto.ProductGenericRequest;
import com.pharmsaas.backend.modules.generic.dto.ProductGenericResponse;
import com.pharmsaas.backend.modules.generic.service.ProductGenericService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/product-generics")
public class ProductGenericController {
    private final ProductGenericService service;

    public ProductGenericController(ProductGenericService service) {
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
    public List<ProductGenericResponse> list(
        @PathVariable Long tenantId, Authentication authentication
    ) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductGenericResponse create(
        @PathVariable Long tenantId,
        @Valid @RequestBody ProductGenericRequest request,
        Authentication authentication
    ) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @PutMapping("/{genericId}")
    public ProductGenericResponse update(
        @PathVariable Long tenantId,
        @PathVariable Long genericId,
        @Valid @RequestBody ProductGenericRequest request,
        Authentication authentication
    ) {
        return service.update(
            tenantId, currentUserId(authentication), genericId, request
        );
    }

    @DeleteMapping("/{genericId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
        @PathVariable Long tenantId,
        @PathVariable Long genericId,
        Authentication authentication
    ) {
        service.archive(tenantId, currentUserId(authentication), genericId);
    }
}
