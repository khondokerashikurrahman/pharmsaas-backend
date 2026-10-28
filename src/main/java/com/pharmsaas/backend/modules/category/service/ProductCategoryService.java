package com.pharmsaas.backend.modules.category.service;

import com.pharmsaas.backend.modules.category.dto.ProductCategoryRequest;
import com.pharmsaas.backend.modules.category.dto.ProductCategoryResponse;
import com.pharmsaas.backend.modules.category.entity.ProductCategory;
import com.pharmsaas.backend.modules.category.repository.ProductCategoryRepository;
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
import java.util.Map;

@Service
public class ProductCategoryService {

    private static final Map<String, List<String>> DEFAULTS = Map.ofEntries(
        Map.entry("MEDICINE", List.of(
            "Pain Relief", "Gastric", "Diabetes", "Allergy",
            "Antibiotic", "Vitamin & Supplement", "Cardiovascular", "Other"
        )),
        Map.entry("OTC", List.of(
            "Cold & Flu", "Pain Relief", "Digestive Health",
            "Oral Care", "First Aid", "Vitamin & Supplement", "Other"
        )),
        Map.entry("COSMETICS", List.of(
            "Skin Care", "Hair Care", "Makeup", "Fragrance", "Other"
        )),
        Map.entry("BABY_CARE", List.of(
            "Baby Food", "Diapers", "Baby Skin Care", "Feeding", "Other"
        )),
        Map.entry("PERSONAL_CARE", List.of(
            "Skin Care", "Hair Care", "Oral Care", "Body Care", "Other"
        )),
        Map.entry("MEDICAL_DEVICE", List.of(
            "Diagnostic", "Monitoring", "First Aid", "Mobility", "Other"
        )),
        Map.entry("HYGIENE", List.of(
            "Sanitary Care", "Disinfectant", "Masks", "Gloves", "Other"
        )),
        Map.entry("GENERAL", List.of(
            "General Goods", "Snacks", "Household", "Other"
        )),
        Map.entry("OTHER", List.of("Other"))
    );

    private final ProductCategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public ProductCategoryService(
        ProductCategoryRepository categoryRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository
    ) {
        this.categoryRepository = categoryRepository;
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
                "Only Tenant Owner or Pharmacy Admin can manage categories"
            );
        }

        return tenant;
    }

    private String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!DEFAULTS.containsKey(type)) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid product type: " + type);
        }
        return type;
    }

    private void seedDefaults(Tenant tenant) {
        // Seed once per tenant. Archived categories will not be recreated on later reads.
        if (categoryRepository.existsByTenant_Id(tenant.getId())) {
            return;
        }

        for (var entry : DEFAULTS.entrySet()) {
            for (String name : entry.getValue()) {
                ProductCategory category = new ProductCategory();
                category.setTenant(tenant);
                category.setProductType(entry.getKey());
                category.setName(name);
                category.setActive(true);
                categoryRepository.save(category);
            }
        }
    }

    @Transactional
    public List<ProductCategoryResponse> list(
        Long tenantId, Long userId, String productType
    ) {
        Tenant tenant = authorize(tenantId, userId, false);
        seedDefaults(tenant);

        String type = productType == null || productType.isBlank()
            ? null : normalizeType(productType);

        List<ProductCategory> categories = type == null
            ? categoryRepository.findByTenant_IdAndActiveTrueOrderByProductTypeAscNameAsc(
                tenantId
            )
            : categoryRepository
                .findByTenant_IdAndProductTypeIgnoreCaseAndActiveTrueOrderByNameAsc(
                    tenantId, type
                );

        return categories.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductCategoryResponse create(
        Long tenantId, Long userId, ProductCategoryRequest request
    ) {
        Tenant tenant = authorize(tenantId, userId, true);
        seedDefaults(tenant);

        String type = normalizeType(request.productType());
        String name = request.name().trim();

        if (exists(tenantId, type, name, null)) {
            throw error(
                HttpStatus.CONFLICT,
                "This category already exists for the selected product type"
            );
        }

        ProductCategory category = new ProductCategory();
        category.setTenant(tenant);
        category.setProductType(type);
        category.setName(name);
        category.setActive(true);

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public ProductCategoryResponse update(
        Long tenantId, Long userId, Long categoryId,
        ProductCategoryRequest request
    ) {
        authorize(tenantId, userId, true);

        String type = normalizeType(request.productType());
        String name = request.name().trim();

        ProductCategory category = categoryRepository
            .findByIdAndTenant_IdAndActiveTrue(categoryId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Category not found"));

        if (exists(tenantId, type, name, categoryId)) {
            throw error(
                HttpStatus.CONFLICT,
                "This category already exists for the selected product type"
            );
        }

        category.setProductType(type);
        category.setName(name);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void archive(Long tenantId, Long userId, Long categoryId) {
        authorize(tenantId, userId, true);

        ProductCategory category = categoryRepository
            .findByIdAndTenant_IdAndActiveTrue(categoryId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Category not found"));

        category.setActive(false);
        categoryRepository.save(category);
    }

    private boolean exists(Long tenantId, String type, String name, Long excludeId) {
        if (excludeId == null) {
            return categoryRepository
                .existsByTenant_IdAndProductTypeIgnoreCaseAndNameIgnoreCase(
                    tenantId, type, name
                );
        }

        return categoryRepository
            .existsByTenant_IdAndProductTypeIgnoreCaseAndNameIgnoreCaseAndIdNot(
                tenantId, type, name, excludeId
            );
    }

    private ProductCategoryResponse toResponse(ProductCategory category) {
        return new ProductCategoryResponse(
            category.getId(),
            category.getTenant().getId(),
            category.getProductType(),
            category.getName(),
            category.isActive(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
