package com.pharmsaas.backend.modules.membership.service;

import com.pharmsaas.backend.modules.membership.dto.TenantMemberCreateRequest;
import com.pharmsaas.backend.modules.membership.dto.TenantMemberCreateResponse;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.user.entity.SystemRole;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class TenantStaffService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private static final Set<MembershipRole> ASSIGNABLE_ROLES = Set.of(
            MembershipRole.PHARMACY_ADMIN,
            MembershipRole.MANAGER,
            MembershipRole.STORE_KEEPER,
            MembershipRole.PHARMACIST,
            MembershipRole.STAFF,
            MembershipRole.CASHIER
    );

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantStaffService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder) {

        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TenantMemberCreateResponse createMember(
            Long tenantId,
            Long actorUserId,
            TenantMemberCreateRequest request) {

        if (tenantId == null || tenantId <= 0
                || actorUserId == null || actorUserId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid tenant and authenticated user IDs are required."
            );
        }

        if (request == null || request.role() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A membership role is required."
            );
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant not found."
                ));

        // Only approved, active pharmacies can manage staff.
        if (tenant.getStatus() == null
                || !"Active".equalsIgnoreCase(tenant.getStatus().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This pharmacy is not active."
            );
        }

        // Verify the acting user has an active membership in THIS tenant.
        TenantMembership actorMembership = membershipRepository
                .findByTenant_IdAndUser_IdAndActiveTrue(
                        tenantId, actorUserId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You do not have an active membership in this pharmacy."
                ));

        MembershipRole actorRole = actorMembership.getRole();

        if (actorRole != MembershipRole.TENANT_OWNER
                && actorRole != MembershipRole.PHARMACY_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Tenant Owner or Pharmacy Admin can create staff accounts."
            );
        }

        MembershipRole requestedRole = request.role();

        // Never let a tenant create a global Super Admin or another Owner.
        if (!ASSIGNABLE_ROLES.contains(requestedRole)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This role cannot be assigned through tenant staff creation."
            );
        }

        // Only the Tenant Owner may appoint a Pharmacy Admin.
        if (requestedRole == MembershipRole.PHARMACY_ADMIN
                && actorRole != MembershipRole.TENANT_OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Tenant Owner can appoint a Pharmacy Admin."
            );
        }

        String fullName = request.fullName() == null
                ? ""
                : request.fullName().trim();

        String email = request.email() == null
                ? ""
                : request.email().trim().toLowerCase(Locale.ROOT);

        String password = request.password();

        if (fullName.isBlank() || fullName.length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid full name is required."
            );
        }

        if (email.isBlank()
                || email.length() > 255
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid email address is required."
            );
        }

        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain 8 to 72 characters."
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }

        // The new account is automatically attached to the route's tenant.
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setSystemRole(SystemRole.USER);
        user.setTenant(tenant);

        User savedUser = userRepository.save(user);

        TenantMembership membership = new TenantMembership();
        membership.setTenant(tenant);
        membership.setUser(savedUser);
        membership.setRole(requestedRole);
        membership.setActive(true);

        TenantMembership savedMembership =
                membershipRepository.save(membership);

        return new TenantMemberCreateResponse(
                savedMembership.getId(),
                savedUser.getId(),
                tenant.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedMembership.getRole(),
                savedMembership.isActive()
        );
    }
}
