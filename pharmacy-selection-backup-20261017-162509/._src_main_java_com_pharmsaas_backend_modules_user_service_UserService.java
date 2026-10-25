
package com.pharmsaas.backend.modules.user.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.user.dto.LoginRequest;
import com.pharmsaas.backend.modules.user.dto.LoginResponse;
import com.pharmsaas.backend.modules.user.dto.RegisterRequest;
import com.pharmsaas.backend.modules.user.dto.UserResponse;
import com.pharmsaas.backend.modules.user.entity.SystemRole;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.user.repository.UserRepository;
import com.pharmsaas.backend.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final TenantMembershipRepository tenantMembershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;

    public UserService(
            TenantMembershipRepository tenantMembershipRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantRepository tenantRepository,
            JwtService jwtService) {

        this.tenantMembershipRepository = tenantMembershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
        this.jwtService = jwtService;
    }

    // =====================================================
    // REGISTER USER
    // =====================================================

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (request == null) {
            throw badRequest("Registration request is required");
        }

        String fullName = request.fullName() == null
                ? ""
                : request.fullName().trim();

        String email = normalizeEmail(request.email());

        validateNameAndEmail(fullName, email);
        validatePassword(request.password());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        Tenant tenant = findTenant(request.tenantId());

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setEnabled(true);
        user.setSystemRole(SystemRole.USER);
        user.setTenant(tenant);

        User savedUser = userRepository.save(user);

        // Default operational role for this registration workflow.
        // Tenant-owner registration should use a separate,
        // protected tenant-onboarding workflow.
        TenantMembership membership = new TenantMembership();
        membership.setTenant(tenant);
        membership.setUser(savedUser);
        membership.setRole(MembershipRole.STAFF);
        membership.setActive(true);

        tenantMembershipRepository.save(membership);

        return toResponse(savedUser);
    }

    // =====================================================
    // GET ALL USERS
    // =====================================================

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // GET USER BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        return toResponse(findUser(id));
    }

    // =====================================================
    // UPDATE USER
    // =====================================================

    @Transactional
    public UserResponse updateUser(
            Long id,
            RegisterRequest request) {

        if (request == null) {
            throw badRequest("Update request is required");
        }

        User user = findUser(id);

        String fullName = request.fullName() == null
                ? ""
                : request.fullName().trim();

        String email = normalizeEmail(request.email());

        validateNameAndEmail(fullName, email);

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already used by another user"
            );
        }

        Tenant requestedTenant = findTenant(request.tenantId());

        // Do not silently move a user between tenants.
        // Tenant transfers must be handled through a dedicated,
        // authorized membership-management workflow.
        if (user.getTenant() == null
                || !user.getTenant().getId()
                .equals(requestedTenant.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Changing a user's tenant requires the authorized membership workflow"
            );
        }

        user.setFullName(fullName);
        user.setEmail(email);

        // Blank password means keep the existing password.
        if (request.password() != null
                && !request.password().isBlank()) {

            validatePassword(request.password());

            user.setPasswordHash(
                    passwordEncoder.encode(request.password())
            );
        }

        return toResponse(userRepository.save(user));
    }

    // =====================================================
    // DELETE / DISABLE USER
    // =====================================================

    @Transactional
    public void deleteUser(Long id) {

        User user = findUser(id);

        // Disable instead of hard-deleting: memberships and
        // related records may reference this user.
        user.setEnabled(false);
        userRepository.save(user);
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        if (request == null
                || request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (!user.isEnabled()
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // -------------------------------------------------
        // GLOBAL SUPER ADMIN
        // -------------------------------------------------

        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {

            String token = jwtService.generateToken(
                    user.getId(),
                    user.getEmail()
            );

            return new LoginResponse(
                    token,
                    "Bearer",
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    null,
                    "SUPER_ADMIN",
                    null,
                    null
            );
        }

        // -------------------------------------------------
        // TENANT USER
        // -------------------------------------------------

        List<TenantMembership> memberships =
                tenantMembershipRepository
                        .findByUser_IdAndActiveTrue(user.getId());

        if (memberships.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No active tenant membership found"
            );
        }

        // Never silently select an arbitrary tenant when the
        // user belongs to multiple active tenants.
        if (memberships.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple active tenant memberships found; tenant selection is required"
            );
        }

        TenantMembership membership = memberships.get(0);

        if (membership.getRole() == null
                || membership.getTenant() == null
                || membership.getTenant().getId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid tenant membership configuration"
            );
        }

        // A pharmacy cannot log in until Super Admin activates it.
        String tenantStatus = membership.getTenant().getStatus();

        if (tenantStatus == null
                || !"Active".equalsIgnoreCase(tenantStatus.trim())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your pharmacy is awaiting Super Admin approval."
            );
        }

        String frontendRole = switch (membership.getRole()) {
            case TENANT_OWNER, PHARMACY_ADMIN -> "TENANT_ADMIN";
            case MANAGER -> "MANAGER";
            case STORE_KEEPER -> "STORE_KEEPER";
            case PHARMACIST, STAFF, CASHIER -> "STAFF";
            case SUPER_ADMIN -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Global Super Admin must use the system role"
            );
        };

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail()
        );

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                membership.getTenant().getId(),
                frontendRole,
                membership.getTenant().getStatus(),
                    membership.getRole().name()
            );
    }

    // =====================================================
    // FIND USER
    // =====================================================

    private User findUser(Long id) {

        if (id == null || id <= 0) {
            throw badRequest("A valid user ID is required");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with ID: " + id
                ));
    }

    // =====================================================
    // FIND TENANT
    // =====================================================

    private Tenant findTenant(Long tenantId) {

        if (tenantId == null || tenantId <= 0) {
            throw badRequest("A valid tenant ID is required");
        }

        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant not found"
                ));
    }

    // =====================================================
    // VALIDATE NAME AND EMAIL
    // =====================================================

    private void validateNameAndEmail(
            String fullName,
            String email) {

        if (fullName.isBlank()) {
            throw badRequest("Full name is required");
        }

        if (fullName.length() > 150) {
            throw badRequest("Full name must not exceed 150 characters");
        }

        if (email.isBlank()) {
            throw badRequest("Email is required");
        }

        if (email.length() > 255
                || !email.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw badRequest("A valid email address is required");
        }
    }

    // =====================================================
    // VALIDATE PASSWORD
    // =====================================================

    private void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw badRequest("Password is required");
        }

        if (password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {

            throw badRequest(
                    "Password must contain 8 to 72 characters"
            );
        }
    }

    // =====================================================
    // NORMALIZE EMAIL
    // =====================================================

    private String normalizeEmail(String email) {

        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }

    // =====================================================
    // BAD REQUEST HELPER
    // =====================================================

    private ResponseStatusException badRequest(String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    // =====================================================
    // ENTITY TO RESPONSE DTO
    // =====================================================

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.isEnabled()
        );
    }
}
