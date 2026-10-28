package com.pharmsaas.backend.modules.company.service;

import com.pharmsaas.backend.modules.company.dto.CompanyRequest;
import com.pharmsaas.backend.modules.company.dto.CompanyResponse;
import com.pharmsaas.backend.modules.company.entity.ManufacturerCompany;
import com.pharmsaas.backend.modules.company.repository.ManufacturerCompanyRepository;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ManufacturerCompanyService {
    private static final Set<MembershipRole> READ_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER,
        MembershipRole.STORE_KEEPER
    );

    private static final Set<MembershipRole> WRITE_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER
    );

    private final ManufacturerCompanyRepository companyRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public ManufacturerCompanyService(
        ManufacturerCompanyRepository companyRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository
    ) {
        this.companyRepository = companyRepository;
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
                HttpStatus.FORBIDDEN, "Active tenant membership required"
            ));

        Set<MembershipRole> allowed = write ? WRITE_ROLES : READ_ROLES;
        if (membership.getRole() == null || !allowed.contains(membership.getRole())) {
            throw error(
                HttpStatus.FORBIDDEN,
                write
                    ? "Your role cannot manage companies"
                    : "Your role cannot access companies"
            );
        }
        return tenant;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list(Long tenantId, Long userId) {
        authorize(tenantId, userId, false);
        return companyRepository.findAllByTenant_IdOrderByNameAsc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(Long tenantId, Long userId, Long companyId) {
        authorize(tenantId, userId, false);
        return toResponse(findCompany(tenantId, companyId));
    }

    @Transactional
    public CompanyResponse create(
        Long tenantId, Long userId, CompanyRequest request
    ) {
        Tenant tenant = authorize(tenantId, userId, true);
        validate(request);
        String name = request.name().trim();

        if (companyRepository.existsByTenant_IdAndNameIgnoreCase(tenantId, name)) {
            throw error(HttpStatus.CONFLICT, "This company already exists");
        }

        ManufacturerCompany company = new ManufacturerCompany();
        company.setTenant(tenant);
        company.setCompanyCode(generateCode(tenantId));
        apply(company, request, true);
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(
        Long tenantId, Long userId, Long companyId, CompanyRequest request
    ) {
        authorize(tenantId, userId, true);
        validate(request);
        ManufacturerCompany company = findCompany(tenantId, companyId);
        String name = request.name().trim();

        if (companyRepository.existsByTenant_IdAndNameIgnoreCaseAndIdNot(
            tenantId, name, companyId
        )) {
            throw error(HttpStatus.CONFLICT, "This company already exists");
        }

        apply(company, request, false);
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse deactivate(
        Long tenantId, Long userId, Long companyId
    ) {
        authorize(tenantId, userId, true);
        ManufacturerCompany company = findCompany(tenantId, companyId);
        company.setStatus("Inactive");
        return toResponse(companyRepository.save(company));
    }

    private ManufacturerCompany findCompany(Long tenantId, Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Valid company ID is required");
        }
        return companyRepository.findByIdAndTenant_Id(companyId, tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Company not found for this tenant"
            ));
    }

    private void validate(CompanyRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "Company name is required");
        }

        checkLength(request.name(), 180, "Company name");
        checkLength(request.contactPerson(), 120, "Contact person");
        checkLength(request.phone(), 30, "Phone");
        checkLength(request.email(), 180, "Email");
        checkLength(request.address(), 500, "Address");

        String status = request.status();
        if (status != null && !status.isBlank()
            && !status.equalsIgnoreCase("Active")
            && !status.equalsIgnoreCase("Inactive")) {
            throw error(HttpStatus.BAD_REQUEST, "Status must be Active or Inactive");
        }
    }

    private void checkLength(String value, int max, String field) {
        if (value != null && value.trim().length() > max) {
            throw error(HttpStatus.BAD_REQUEST, field + " cannot exceed " + max + " characters");
        }
    }

    private void apply(
        ManufacturerCompany company, CompanyRequest request, boolean creating
    ) {
        company.setName(request.name().trim());
        company.setContactPerson(clean(request.contactPerson()));
        company.setPhone(clean(request.phone()));
        company.setEmail(clean(request.email()));
        company.setAddress(clean(request.address()));

        if (request.status() != null && !request.status().isBlank()) {
            company.setStatus(
                request.status().trim().equalsIgnoreCase("Inactive")
                    ? "Inactive" : "Active"
            );
        } else if (creating) {
            company.setStatus("Active");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String generateCode(Long tenantId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "CMP-" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 8)
                .toUpperCase(Locale.ROOT);
            if (!companyRepository.existsByTenant_IdAndCompanyCode(tenantId, code)) {
                return code;
            }
        }
        throw error(HttpStatus.CONFLICT, "Could not generate a unique company code");
    }

    private CompanyResponse toResponse(ManufacturerCompany company) {
        return new CompanyResponse(
            company.getId(),
            company.getTenant().getId(),
            company.getCompanyCode(),
            company.getName(),
            company.getContactPerson(),
            company.getPhone(),
            company.getEmail(),
            company.getAddress(),
            company.getStatus(),
            company.getCreatedAt(),
            company.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
