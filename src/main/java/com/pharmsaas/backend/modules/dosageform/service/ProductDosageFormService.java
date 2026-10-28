package com.pharmsaas.backend.modules.dosageform.service;

import com.pharmsaas.backend.modules.dosageform.dto.DosageFormRequest;
import com.pharmsaas.backend.modules.dosageform.dto.DosageFormResponse;
import com.pharmsaas.backend.modules.dosageform.entity.ProductDosageForm;
import com.pharmsaas.backend.modules.dosageform.repository.ProductDosageFormRepository;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductDosageFormService {
    private static final List<String> DEFAULTS = List.of(
        "Tablet",
        "Capsule",
        "Syrup",
        "Suspension",
        "Injection",
        "Cream",
        "Ointment",
        "Gel",
        "Lotion",
        "Drops",
        "Eye Drops",
        "Ear Drops",
        "Nasal Drops",
        "Inhaler",
        "Spray",
        "Powder",
        "Solution",
        "Sachet",
        "Suppository",
        "Patch",
        "Other"
    );

    private final ProductDosageFormRepository dosageRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public ProductDosageFormService(
        ProductDosageFormRepository dosageRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository
    ) {
        this.dosageRepository = dosageRepository;
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

        if (write &&
            membership.getRole() != MembershipRole.TENANT_OWNER &&
            membership.getRole() != MembershipRole.PHARMACY_ADMIN) {
            throw error(
                HttpStatus.FORBIDDEN,
                "Only Tenant Owner or Pharmacy Admin can manage dosage forms"
            );
        }

        return tenant;
    }

    private void seedDefaults(Tenant tenant) {
        // Seed only on a tenant's first dosage-form access.
        // Do not recreate defaults if the tenant later archives all of them.
        if (dosageRepository.existsByTenant_Id(tenant.getId())) {
            return;
        }

        for (String name : DEFAULTS) {
            ProductDosageForm form = new ProductDosageForm();
            form.setTenant(tenant);
            form.setName(name);
            form.setActive(true);
            dosageRepository.save(form);
        }
    }

    @Transactional
    public List<DosageFormResponse> list(Long tenantId, Long userId) {
        Tenant tenant = authorize(tenantId, userId, false);
        seedDefaults(tenant);

        return dosageRepository
            .findByTenant_IdAndActiveTrueOrderByNameAsc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DosageFormResponse create(
        Long tenantId, Long userId, DosageFormRequest request
    ) {
        Tenant tenant = authorize(tenantId, userId, true);
        seedDefaults(tenant);

        String name = request.name().trim();
        if (dosageRepository.existsByTenant_IdAndNameIgnoreCase(tenantId, name)) {
            throw error(HttpStatus.CONFLICT, "This dosage form already exists");
        }

        ProductDosageForm form = new ProductDosageForm();
        form.setTenant(tenant);
        form.setName(name);
        form.setActive(true);
        return toResponse(dosageRepository.save(form));
    }

    @Transactional
    public DosageFormResponse update(
        Long tenantId, Long userId, Long dosageFormId,
        DosageFormRequest request
    ) {
        authorize(tenantId, userId, true);
        String name = request.name().trim();

        ProductDosageForm form = dosageRepository
            .findByIdAndTenant_IdAndActiveTrue(dosageFormId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Dosage form not found"));

        if (dosageRepository.existsByTenant_IdAndNameIgnoreCaseAndIdNot(
            tenantId, name, dosageFormId
        )) {
            throw error(HttpStatus.CONFLICT, "This dosage form already exists");
        }

        form.setName(name);
        return toResponse(dosageRepository.save(form));
    }

    @Transactional
    public void archive(Long tenantId, Long userId, Long dosageFormId) {
        authorize(tenantId, userId, true);

        ProductDosageForm form = dosageRepository
            .findByIdAndTenant_IdAndActiveTrue(dosageFormId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Dosage form not found"));

        form.setActive(false);
        dosageRepository.save(form);
    }

    private DosageFormResponse toResponse(ProductDosageForm form) {
        return new DosageFormResponse(
            form.getId(),
            form.getTenant().getId(),
            form.getName(),
            form.isActive(),
            form.getCreatedAt(),
            form.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
