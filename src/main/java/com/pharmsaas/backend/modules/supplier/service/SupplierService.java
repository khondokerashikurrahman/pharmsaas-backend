package com.pharmsaas.backend.modules.supplier.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.supplier.dto.SupplierRequest;
import com.pharmsaas.backend.modules.supplier.dto.SupplierResponse;
import com.pharmsaas.backend.modules.supplier.entity.Supplier;
import com.pharmsaas.backend.modules.supplier.repository.SupplierRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SupplierService {
    private static final Set<MembershipRole> WRITE_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER
    );

    private static final Set<MembershipRole> READ_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER,
        MembershipRole.STORE_KEEPER
    );

    private static final Set<String> STATUSES =
        Set.of("Active", "Inactive", "Blocked");

    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public SupplierService(
            SupplierRepository supplierRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository) {
        this.supplierRepository = supplierRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    private Tenant authorize(Long tenantId, Long userId, boolean write) {
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Valid tenant and user IDs are required");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null ||
                !"ACTIVE".equalsIgnoreCase(tenant.getStatus().trim())) {
            throw error(HttpStatus.FORBIDDEN, "Tenant must be Active");
        }

        var membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> error(
                HttpStatus.FORBIDDEN, "Active tenant membership required"));

        Set<MembershipRole> allowed = write ? WRITE_ROLES : READ_ROLES;
        if (membership.getRole() == null ||
                !allowed.contains(membership.getRole())) {
            throw error(
                HttpStatus.FORBIDDEN,
                write
                    ? "Only tenant owners, pharmacy admins or managers can manage suppliers"
                    : "Your role cannot access suppliers");
        }

        return tenant;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list(Long tenantId, Long userId) {
        authorize(tenantId, userId, false);
        return supplierRepository.findAllByTenant_IdOrderByNameAsc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(Long tenantId, Long userId, Long supplierId) {
        authorize(tenantId, userId, false);
        return toResponse(findSupplier(tenantId, supplierId));
    }

    @Transactional
    public SupplierResponse create(
            Long tenantId, Long userId, SupplierRequest request) {
        Tenant tenant = authorize(tenantId, userId, true);
        validate(request);

        String name = request.name().trim();
        if (supplierRepository.findByTenant_IdAndNameIgnoreCase(tenantId, name)
                .isPresent()) {
            throw error(
                HttpStatus.CONFLICT,
                "A supplier with this name already exists for this tenant");
        }

        Supplier supplier = new Supplier();
        supplier.setTenant(tenant);
        supplier.setSupplierCode(generateCode(tenantId));
        apply(supplier, request, true);

        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(
            Long tenantId, Long userId, Long supplierId,
            SupplierRequest request) {
        authorize(tenantId, userId, true);
        validate(request);

        Supplier supplier = findSupplier(tenantId, supplierId);
        String name = request.name().trim();

        var duplicate = supplierRepository
            .findByTenant_IdAndNameIgnoreCase(tenantId, name);
        if (duplicate.isPresent() &&
                !duplicate.get().getId().equals(supplierId)) {
            throw error(
                HttpStatus.CONFLICT,
                "A supplier with this name already exists for this tenant");
        }

        apply(supplier, request, false);
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse deactivate(
            Long tenantId, Long userId, Long supplierId) {
        authorize(tenantId, userId, true);
        Supplier supplier = findSupplier(tenantId, supplierId);

        if (!"Blocked".equalsIgnoreCase(supplier.getStatus())) {
            supplier.setStatus("Inactive");
        }

        return toResponse(supplierRepository.save(supplier));
    }

    private Supplier findSupplier(Long tenantId, Long supplierId) {
        if (supplierId == null || supplierId <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Valid supplier ID is required");
        }

        return supplierRepository.findByIdAndTenant_Id(supplierId, tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Supplier not found for this tenant"));
    }

    private void validate(SupplierRequest r) {
        if (r == null || r.name() == null || r.name().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "Supplier name is required");
        }
        checkLength(r.name(), 180, "Supplier name", false);
        checkLength(r.companyName(), 180, "Company name", true);
        checkLength(r.contactPerson(), 120, "Contact person", true);
        checkLength(r.phone(), 30, "Phone", true);
        checkLength(r.email(), 180, "Email", true);
        checkLength(r.address(), 500, "Address", true);

        if (r.email() != null && !r.email().isBlank()
                && !r.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid email address");
        }

        money(r.openingBalance(), "Opening balance");
        money(r.creditLimit(), "Credit limit");

        if (r.status() != null && !r.status().isBlank()
                && STATUSES.stream().noneMatch(
                    s -> s.equalsIgnoreCase(r.status().trim()))) {
            throw error(
                HttpStatus.BAD_REQUEST,
                "Status must be Active, Inactive or Blocked");
        }
    }

    private void checkLength(
            String value, int maximum, String field, boolean optional) {
        if (value == null || value.isBlank()) {
            if (!optional) {
                throw error(HttpStatus.BAD_REQUEST, field + " is required");
            }
            return;
        }
        if (value.trim().length() > maximum) {
            throw error(
                HttpStatus.BAD_REQUEST,
                field + " cannot exceed " + maximum + " characters");
        }
    }

    private BigDecimal money(BigDecimal value, String field) {
        if (value == null) return BigDecimal.ZERO.setScale(2);
        if (value.signum() < 0) {
            throw error(HttpStatus.BAD_REQUEST, field + " cannot be negative");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw error(
                HttpStatus.BAD_REQUEST,
                field + " cannot have more than two decimal places");
        }
        if (value.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw error(HttpStatus.BAD_REQUEST, field + " is too large");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void apply(Supplier s, SupplierRequest r, boolean creating) {
        s.setName(r.name().trim());
        s.setCompanyName(clean(r.companyName()));
        s.setContactPerson(clean(r.contactPerson()));
        s.setPhone(clean(r.phone()));
        s.setEmail(clean(r.email()));
        s.setAddress(clean(r.address()));
        s.setOpeningBalance(money(r.openingBalance(), "Opening balance"));
        s.setCreditLimit(money(r.creditLimit(), "Credit limit"));

        if (r.status() != null && !r.status().isBlank()) {
            String requested = r.status().trim();
            s.setStatus(STATUSES.stream()
                .filter(status -> status.equalsIgnoreCase(requested))
                .findFirst().orElseThrow());
        } else if (creating) {
            s.setStatus("Active");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String generateCode(Long tenantId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "SUP-" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10)
                .toUpperCase(Locale.ROOT);
            if (!supplierRepository.existsByTenant_IdAndSupplierCode(
                    tenantId, code)) {
                return code;
            }
        }
        throw error(
            HttpStatus.CONFLICT,
            "Could not allocate a unique supplier code; please retry");
    }

    private SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(
            s.getId(),
            s.getTenant().getId(),
            s.getSupplierCode(),
            s.getName(),
            s.getCompanyName(),
            s.getContactPerson(),
            s.getPhone(),
            s.getEmail(),
            s.getAddress(),
            s.getOpeningBalance(),
            s.getCreditLimit(),
            s.getStatus(),
            s.getCreatedAt(),
            s.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
