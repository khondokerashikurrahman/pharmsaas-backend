package com.pharmsaas.backend.modules.category.controller;

import com.pharmsaas.backend.modules.category.dto.ProductCategoryRequest;
import com.pharmsaas.backend.modules.category.dto.ProductCategoryResponse;
import com.pharmsaas.backend.modules.category.service.ProductCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/product-categories")
public class ProductCategoryController {

    private final ProductCategoryService service;

    public ProductCategoryController(ProductCategoryService service) {
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
    public List<ProductCategoryResponse> list(
        @PathVariable Long tenantId,
        @RequestParam(required = false) String productType,
        Authentication authentication
    ) {
        return service.list(tenantId, currentUserId(authentication), productType);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCategoryResponse create(
        @PathVariable Long tenantId,
        @Valid @RequestBody ProductCategoryRequest request,
        Authentication authentication
    ) {
        return service.create(tenantId, currentUserId(authentication), request);
    }

    @PutMapping("/{categoryId}")
    public ProductCategoryResponse update(
        @PathVariable Long tenantId,
        @PathVariable Long categoryId,
        @Valid @RequestBody ProductCategoryRequest request,
        Authentication authentication
    ) {
        return service.update(
            tenantId, currentUserId(authentication), categoryId, request
        );
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
        @PathVariable Long tenantId,
        @PathVariable Long categoryId,
        Authentication authentication
    ) {
        service.archive(tenantId, currentUserId(authentication), categoryId);
    }
}
