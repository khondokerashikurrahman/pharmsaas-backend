package com.pharmsaas.backend.modules.tenant.service;


import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.dto.PublicTenantRegistrationRequest;
import com.pharmsaas.backend.modules.tenant.dto.PublicTenantRegistrationResponse;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.user.repository.UserRepository;
import com.pharmsaas.backend.modules.store.service.StoreService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class PublicTenantRegistrationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final StoreService storeService;
    private final PasswordEncoder passwordEncoder;

    public PublicTenantRegistrationService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            StoreService storeService,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.storeService = storeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PublicTenantRegistrationResponse register(
            PublicTenantRegistrationRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }

        // Generate a unique tenant code on the backend.
        String tenantCode;
        do {
            tenantCode = "PH-" +
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 10)
                            .toUpperCase(Locale.ROOT);
        } while (tenantRepository.existsByTenantCode(tenantCode));

        // 1. Create tenant.
        Tenant tenant = new Tenant();
        tenant.setTenantCode(tenantCode);
        tenant.setName(request.pharmacyName().trim());
        tenant.setOwnerName(request.ownerName().trim());
        tenant.setEmail(email);
        tenant.setPhone(
                request.phone() == null ? null : request.phone().trim()
        );
        tenant.setAddress(
                request.address() == null ? null : request.address().trim()
        );

        // Public registration cannot choose its own approval status.
        tenant.setStatus("Pending");

        Tenant savedTenant = tenantRepository.save(tenant);

        storeService.ensureDefaultStore(savedTenant);

        // 2. Create the owner account.
        User owner = new User();
        owner.setFullName(request.ownerName().trim());
        owner.setEmail(email);
        owner.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        owner.setEnabled(true);
        owner.setTenant(savedTenant);

        User savedOwner = userRepository.save(owner);

        // 3. Assign the tenant owner membership.
        TenantMembership membership = new TenantMembership();
        membership.setTenant(savedTenant);
        membership.setUser(savedOwner);
        membership.setRole(MembershipRole.TENANT_OWNER);
        membership.setActive(true);

        membershipRepository.save(membership);

        return new PublicTenantRegistrationResponse(
                savedTenant.getId(),
                savedTenant.getTenantCode(),
                savedOwner.getId(),
                savedTenant.getName(),
                savedOwner.getEmail(),
                savedTenant.getStatus(),
                "Registration submitted successfully. Your pharmacy is pending approval."
        );
    }
}

