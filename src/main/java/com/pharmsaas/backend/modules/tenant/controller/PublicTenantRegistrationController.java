package com.pharmsaas.backend.modules.tenant.controller;


import com.pharmsaas.backend.modules.tenant.dto.PublicTenantRegistrationRequest;
import com.pharmsaas.backend.modules.tenant.dto.PublicTenantRegistrationResponse;
import com.pharmsaas.backend.modules.tenant.service.PublicTenantRegistrationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicTenantRegistrationController {

    private final PublicTenantRegistrationService registrationService;

    public PublicTenantRegistrationController(
            PublicTenantRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicTenantRegistrationResponse register(
            @Valid @RequestBody PublicTenantRegistrationRequest request) {

        throw new ResponseStatusException(
                HttpStatus.GONE,
                "Direct registration is no longer available. Use the subscription registration process."
        );
    }
}

