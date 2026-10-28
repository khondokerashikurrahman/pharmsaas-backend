package com.pharmsaas.backend.modules.generic.service;

import com.pharmsaas.backend.modules.generic.dto.ProductGenericRequest;
import com.pharmsaas.backend.modules.generic.dto.ProductGenericResponse;
import com.pharmsaas.backend.modules.generic.entity.ProductGeneric;
import com.pharmsaas.backend.modules.generic.repository.ProductGenericRepository;
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
public class ProductGenericService {
    private final ProductGenericRepository genericRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public ProductGenericService(
        ProductGenericRepository genericRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository
    ) {
        this.genericRepository = genericRepository;
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
            !tenant.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw error(HttpStatus.FORBIDDEN, "Tenant is not active");
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
                "Only Tenant Owner or Pharmacy Admin can manage generic names"
            );
        }

        return tenant;
    }

    @Transactional(readOnly = true)
    public List<ProductGenericResponse> list(Long tenantId, Long userId) {
        authorize(tenantId, userId, false);
        return genericRepository
            .findByTenant_IdAndActiveTrueOrderByNameAsc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductGenericResponse create(
        Long tenantId, Long userId, ProductGenericRequest request
    ) {
        Tenant tenant = authorize(tenantId, userId, true);
        String name = request.name().trim();

        if (genericRepository.existsByTenant_IdAndNameIgnoreCase(tenantId, name)) {
            throw error(HttpStatus.CONFLICT, "This generic name already exists");
        }

        ProductGeneric generic = new ProductGeneric();
        generic.setTenant(tenant);
        generic.setName(name);
        generic.setActive(true);
        return toResponse(genericRepository.save(generic));
    }

    @Transactional
    public ProductGenericResponse update(
        Long tenantId, Long userId, Long genericId,
        ProductGenericRequest request
    ) {
        authorize(tenantId, userId, true);
        String name = request.name().trim();

        ProductGeneric generic = genericRepository
            .findByIdAndTenant_IdAndActiveTrue(genericId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Generic name not found"));

        if (genericRepository.existsByTenant_IdAndNameIgnoreCaseAndIdNot(
            tenantId, name, genericId
        )) {
            throw error(HttpStatus.CONFLICT, "This generic name already exists");
        }

        generic.setName(name);
        return toResponse(genericRepository.save(generic));
    }

    @Transactional
    public void archive(Long tenantId, Long userId, Long genericId) {
        authorize(tenantId, userId, true);

        ProductGeneric generic = genericRepository
            .findByIdAndTenant_IdAndActiveTrue(genericId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Generic name not found"));

        generic.setActive(false);
        genericRepository.save(generic);
    }

    private ProductGenericResponse toResponse(ProductGeneric generic) {
        return new ProductGenericResponse(
            generic.getId(),
            generic.getTenant().getId(),
            generic.getName(),
            generic.isActive(),
            generic.getCreatedAt(),
            generic.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
