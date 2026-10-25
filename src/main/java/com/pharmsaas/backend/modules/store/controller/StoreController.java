package com.pharmsaas.backend.modules.store.controller;


import com.pharmsaas.backend.modules.store.dto.StoreRequest;
import com.pharmsaas.backend.modules.store.dto.StoreResponse;
import com.pharmsaas.backend.modules.store.service.StoreService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    private Long getCurrentUserId(Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please login first."
            );
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user."
            );
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse create(
            @PathVariable Long tenantId,
            @Valid @RequestBody StoreRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        return storeService.create(tenantId, userId, request);
    }

    @GetMapping
    public List<StoreResponse> getAll(
            @PathVariable Long tenantId,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        return storeService.getAll(tenantId, userId);
    }

    @GetMapping("/{storeId}")
    public StoreResponse getById(
            @PathVariable Long tenantId,
            @PathVariable Long storeId,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        return storeService.getById(tenantId, storeId, userId);
    }

    @PutMapping("/{storeId}")
    public StoreResponse update(
            @PathVariable Long tenantId,
            @PathVariable Long storeId,
            @Valid @RequestBody StoreRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        return storeService.update(
                tenantId, storeId, userId, request);
    }

    @DeleteMapping("/{storeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long tenantId,
            @PathVariable Long storeId,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        storeService.delete(tenantId, storeId, userId);
    }
}
