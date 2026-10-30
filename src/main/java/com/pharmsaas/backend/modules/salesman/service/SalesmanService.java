package com.pharmsaas.backend.modules.salesman.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.salesman.dto.SalesmanRequest;
import com.pharmsaas.backend.modules.salesman.dto.SalesmanResponse;
import com.pharmsaas.backend.modules.salesman.entity.Salesman;
import com.pharmsaas.backend.modules.salesman.repository.SalesmanRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
public class SalesmanService {

    private static final Set<MembershipRole> WRITE_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER
    );

    private static final Set<MembershipRole> READ_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER,
        MembershipRole.STORE_KEEPER,
        MembershipRole.PHARMACIST,
        MembershipRole.STAFF,
        MembershipRole.CASHIER
    );

    private final SalesmanRepository salesmanRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public SalesmanService(
            SalesmanRepository salesmanRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository) {
        this.salesmanRepository = salesmanRepository;
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

        if (membership.getRole() == null || !allowed.contains(membership.getRole())) {
            throw error(HttpStatus.FORBIDDEN,
                write ? "Your role cannot manage salesmen"
                      : "Your role cannot access salesmen");
        }

        return tenant;
    }

    @Transactional(readOnly = true)
    public List<SalesmanResponse> list(Long tenantId, Long userId) {
        authorize(tenantId, userId, false);

        return salesmanRepository.findAllByTenant_IdOrderByNameAsc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalesmanResponse get(Long tenantId, Long userId, Long salesmanId) {
        authorize(tenantId, userId, false);
        return toResponse(findSalesman(tenantId, salesmanId));
    }

    @Transactional
    public SalesmanResponse create(
            Long tenantId, Long userId, SalesmanRequest request) {
        Tenant tenant = authorize(tenantId, userId, true);
        validate(request);

        String employeeId = clean(request.employeeId());

        if (salesmanRepository
                .findByTenant_IdAndEmployeeIdIgnoreCase(tenantId, employeeId)
                .isPresent()) {
            throw error(HttpStatus.CONFLICT,
                "This employee ID already exists for this tenant");
        }

        Salesman salesman = new Salesman();
        salesman.setTenant(tenant);
        apply(salesman, request, true);

        return toResponse(salesmanRepository.save(salesman));
    }

    @Transactional
    public SalesmanResponse update(
            Long tenantId, Long userId, Long salesmanId,
            SalesmanRequest request) {
        authorize(tenantId, userId, true);
        validate(request);

        Salesman salesman = findSalesman(tenantId, salesmanId);
        String employeeId = clean(request.employeeId());

        var duplicate = salesmanRepository
            .findByTenant_IdAndEmployeeIdIgnoreCase(tenantId, employeeId);

        if (duplicate.isPresent() &&
                !duplicate.get().getId().equals(salesmanId)) {
            throw error(HttpStatus.CONFLICT,
                "This employee ID already exists for this tenant");
        }

        apply(salesman, request, false);
        return toResponse(salesmanRepository.save(salesman));
    }

    @Transactional
    public SalesmanResponse deactivate(
            Long tenantId, Long userId, Long salesmanId) {
        authorize(tenantId, userId, true);

        Salesman salesman = findSalesman(tenantId, salesmanId);
        salesman.setStatus("Inactive");

        return toResponse(salesmanRepository.save(salesman));
    }

    private Salesman findSalesman(Long tenantId, Long salesmanId) {
        if (salesmanId == null || salesmanId <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Valid salesman ID is required");
        }

        return salesmanRepository.findByIdAndTenant_Id(salesmanId, tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Salesman not found for this tenant"));
    }

    private void validate(SalesmanRequest request) {
        if (request == null) {
            throw error(HttpStatus.BAD_REQUEST, "Salesman data is required");
        }

        checkLength(request.employeeId(), 30, "Employee ID", false);
        checkLength(request.name(), 180, "Name", false);
        checkLength(request.phone(), 30, "Phone", true);
        checkLength(request.email(), 180, "Email", true);
        checkLength(request.address(), 500, "Address", true);
        checkLength(request.territory(), 120, "Territory", true);

        if (request.email() != null && !request.email().isBlank()
                && !request.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid email address");
        }

        BigDecimal rate = request.commissionRate() == null
            ? BigDecimal.ZERO : request.commissionRate();

        if (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0
                || rate.stripTrailingZeros().scale() > 2) {
            throw error(HttpStatus.BAD_REQUEST,
                "Commission rate must be between 0 and 100 with at most two decimals");
        }

        if (request.status() != null && !request.status().isBlank()
                && !Set.of("Active", "Inactive").stream()
                    .anyMatch(s -> s.equalsIgnoreCase(request.status().trim()))) {
            throw error(HttpStatus.BAD_REQUEST,
                "Status must be Active or Inactive");
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
            throw error(HttpStatus.BAD_REQUEST,
                field + " cannot exceed " + maximum + " characters");
        }
    }

    private void apply(
            Salesman salesman, SalesmanRequest request, boolean creating) {
        salesman.setEmployeeId(clean(request.employeeId()));
        salesman.setName(clean(request.name()));
        salesman.setPhone(clean(request.phone()));
        salesman.setEmail(clean(request.email()));
        salesman.setAddress(clean(request.address()));
        salesman.setTerritory(clean(request.territory()));

        BigDecimal rate = request.commissionRate() == null
            ? BigDecimal.ZERO : request.commissionRate();

        salesman.setCommissionRate(rate.setScale(2, RoundingMode.HALF_UP));

        if (request.status() != null && !request.status().isBlank()) {
            salesman.setStatus(
                request.status().trim().equalsIgnoreCase("Active")
                    ? "Active" : "Inactive");
        } else if (creating) {
            salesman.setStatus("Active");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SalesmanResponse toResponse(Salesman salesman) {
        return new SalesmanResponse(
            salesman.getId(),
            salesman.getTenant().getId(),
            salesman.getEmployeeId(),
            salesman.getName(),
            salesman.getPhone(),
            salesman.getEmail(),
            salesman.getAddress(),
            salesman.getTerritory(),
            salesman.getCommissionRate(),
            salesman.getStatus(),
            salesman.getCreatedAt(),
            salesman.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
