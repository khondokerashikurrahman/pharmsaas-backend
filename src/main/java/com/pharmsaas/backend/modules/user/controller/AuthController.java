
package com.pharmsaas.backend.modules.user.controller;

import com.pharmsaas.backend.modules.user.dto.LoginRequest;
import com.pharmsaas.backend.modules.user.dto.LoginResponse;
import com.pharmsaas.backend.modules.user.dto.RegisterRequest;
import com.pharmsaas.backend.modules.user.dto.UserResponse;
import com.pharmsaas.backend.modules.user.service.UserService;
import com.pharmsaas.backend.modules.user.dto.PlatformUserRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pharmsaas.backend.modules.user.dto.SelectTenantRequest;
import com.pharmsaas.backend.modules.user.dto.TenantOptionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 1. POST: Register new user
    @PostMapping
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @PostMapping("/platform-users")
    public ResponseEntity<UserResponse> createPlatformUser(
            @Valid @RequestBody PlatformUserRequest request) {

        UserResponse response = userService.createPlatformUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 2. GET: Get all users
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 3. GET: Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(userService.getUserById(id));
    }

    // 4. PUT: Update user
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                userService.updateUser(id, request)
        );
    }

    // DELETE: Soft-delete platform user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {

        Long actorId = getCurrentUserId(authentication);

        userService.deleteUser(actorId, id);

        return ResponseEntity.noContent().build();
    }

    // SUSPEND / REACTIVATE platform user
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updatePlatformUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            Authentication authentication) {

        Long actorId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                userService.setPlatformUserEnabled(
                        actorId,
                        id,
                        enabled
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(userService.login(request));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please login first.");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authenticated user.");
        }
    }

    @GetMapping("/my-pharmacies")
    public ResponseEntity<List<TenantOptionResponse>> getMyPharmacies(
            Authentication authentication) {
        return ResponseEntity.ok(
                userService.getMyPharmacies(getCurrentUserId(authentication))
        );
    }

    @PostMapping("/select-tenant")
    public ResponseEntity<LoginResponse> selectTenant(
            @Valid @RequestBody SelectTenantRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                userService.selectTenant(
                        getCurrentUserId(authentication),
                        request.tenantId()
                )
        );
    }
}

