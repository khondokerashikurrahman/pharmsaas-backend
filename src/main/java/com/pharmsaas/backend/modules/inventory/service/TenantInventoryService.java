package com.pharmsaas.backend.modules.inventory.service;

import com.pharmsaas.backend.modules.inventory.dto.InventoryItemRequest;
import com.pharmsaas.backend.modules.inventory.dto.InventoryItemResponse;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import com.pharmsaas.backend.modules.company.repository.ManufacturerCompanyRepository;
import com.pharmsaas.backend.modules.company.entity.ManufacturerCompany;
import com.pharmsaas.backend.modules.dosageform.repository.ProductDosageFormRepository;
import com.pharmsaas.backend.modules.dosageform.entity.ProductDosageForm;
import com.pharmsaas.backend.modules.generic.repository.ProductGenericRepository;
import com.pharmsaas.backend.modules.generic.entity.ProductGeneric;
import com.pharmsaas.backend.modules.category.repository.ProductCategoryRepository;
import com.pharmsaas.backend.modules.category.entity.ProductCategory;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryRepository;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TenantInventoryService {
    private static final Set<String> PRODUCT_TYPES = Set.of(
        "MEDICINE", "OTC", "COSMETICS", "BABY_CARE",
        "PERSONAL_CARE", "MEDICAL_DEVICE", "HYGIENE", "GENERAL", "OTHER"
    );


    private final TenantInventoryRepository inventoryRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;

        private final ProductCategoryRepository categoryRepository;
    private final ProductGenericRepository genericRepository;
    private final ProductDosageFormRepository dosageFormRepository;
    private final ManufacturerCompanyRepository companyRepository;

public TenantInventoryService(
            TenantInventoryRepository inventoryRepository,
            TenantMembershipRepository membershipRepository,
            TenantRepository tenantRepository,
            ProductCategoryRepository categoryRepository,
            ProductGenericRepository genericRepository,
            ProductDosageFormRepository dosageFormRepository,
            ManufacturerCompanyRepository companyRepository) {
        this.inventoryRepository = inventoryRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
            this.categoryRepository = categoryRepository;
        this.genericRepository = genericRepository;
        this.dosageFormRepository = dosageFormRepository;
        this.companyRepository = companyRepository;
}

    private Tenant authorize(Long tenantId, Long userId, boolean writeAccess) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Tenant must be Active");
        }

        TenantMembership membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Active tenant membership required"));

        MembershipRole role = membership.getRole();

        boolean canRead =
            role == MembershipRole.TENANT_OWNER
            || role == MembershipRole.PHARMACY_ADMIN
            || role == MembershipRole.MANAGER
            || role == MembershipRole.STORE_KEEPER;

        boolean canWrite =
            role == MembershipRole.TENANT_OWNER
            || role == MembershipRole.PHARMACY_ADMIN;

        if (writeAccess && !canWrite) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot modify inventory directly");
        }

        if (!writeAccess && !canRead) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot access this inventory");
        }

        return tenant;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> list(Long tenantId, Long userId) {
        authorize(tenantId, userId, false);
        return inventoryRepository.findAllByTenant_IdOrderByNameAsc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse get(Long tenantId, Long userId, Long itemId) {
        authorize(tenantId, userId, false);
        return toResponse(findItem(tenantId, itemId));
    }

    @Transactional
    public InventoryItemResponse create(
            Long tenantId, Long userId, InventoryItemRequest request) {
        Tenant tenant = authorize(tenantId, userId, true);
        validate(request);

        String sku = request.sku().trim();
        if (inventoryRepository.existsByTenant_IdAndSkuIgnoreCase(tenantId, sku)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "SKU already exists in this tenant");
        }

        TenantInventoryItem item = new TenantInventoryItem();
        item.setTenant(tenant);
        apply(item, request, true);

        return toResponse(inventoryRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse update(
            Long tenantId, Long userId, Long itemId,
            InventoryItemRequest request) {
        authorize(tenantId, userId, true);
        validate(request);

        TenantInventoryItem item = findItem(tenantId, itemId);
        String sku = request.sku().trim();

        if (inventoryRepository.existsByTenant_IdAndSkuIgnoreCaseAndIdNot(
                tenantId, sku, itemId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "SKU already exists in this tenant");
        }

        apply(item, request, false);
        return toResponse(inventoryRepository.save(item));
    }

    @Transactional
    public void delete(Long tenantId, Long userId, Long itemId) {
        authorize(tenantId, userId, true);

        TenantInventoryItem item = findItem(tenantId, itemId);
        int currentStock = item.getStockQuantity() == null
            ? 0 : item.getStockQuantity();

        if (currentStock > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot archive medicine while stock remains. Resolve the remaining stock first."
            );
        }

        // Preserve historical references used by inventory movements and reports.
        item.setStatus("INACTIVE");
        inventoryRepository.save(item);
    }

    private TenantInventoryItem findItem(Long tenantId, Long itemId) {
        return inventoryRepository.findByIdAndTenant_Id(itemId, tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory item not found"));
    }

    private void validate(InventoryItemRequest request) {
        if (request == null || request.name() == null
                || request.name().isBlank()
                || request.sku() == null || request.sku().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Name and SKU are required");
        }

        if (request.purchasePrice() == null || request.sellingPrice() == null
                || request.purchasePrice().compareTo(BigDecimal.ZERO) < 0
                || request.sellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Prices must be zero or greater");
        }

        if ((request.stockQuantity() != null && request.stockQuantity() < 0)
                || request.reorderLevel() == null || request.reorderLevel() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Stock and reorder level cannot be negative");
        }

        if (request.description() != null && request.description().length() > 2000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Description cannot exceed 2000 characters");
        }

        if (request.status() != null
                && !request.status().equalsIgnoreCase("ACTIVE")
                && !request.status().equalsIgnoreCase("INACTIVE")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Status must be ACTIVE or INACTIVE");
        }

        if (request.productType() != null && !request.productType().isBlank()
                && !PRODUCT_TYPES.contains(
                    request.productType().trim().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported product type. Use MEDICINE, OTC, COSMETICS, BABY_CARE, PERSONAL_CARE, MEDICAL_DEVICE, HYGIENE, GENERAL or OTHER");
        }
    }

    private void apply(
            TenantInventoryItem item, InventoryItemRequest request,
            boolean creating) {
        item.setName(request.name().trim());
        String requestedType = request.productType();
        item.setProductType(
            requestedType == null || requestedType.isBlank()
                ? (item.getProductType() == null ? "MEDICINE" : item.getProductType())
                : requestedType.trim().toUpperCase(Locale.ROOT)
        );
        item.setGenericName(request.genericName());
        item.setBrandName(request.brandName());
        item.setStrength(request.strength());
        item.setDosageForm(request.dosageForm());
        item.setCategory(request.category() == null || request.category().isBlank()
            ? "Other" : request.category().trim());
        item.setDescription(
            request.description() == null || request.description().isBlank()
                ? null : request.description().trim()
        );
        item.setSku(request.sku().trim());
        item.setBarcode(request.barcode());
        item.setPurchasePrice(request.purchasePrice());
        item.setSellingPrice(request.sellingPrice());
        item.setReorderLevel(request.reorderLevel());

        if (creating) {
            item.setStockQuantity(0);
        }

        item.setStatus(request.status() == null || request.status().isBlank()
            ? "ACTIVE" : request.status().toUpperCase());
    
        syncMasterLinks(item, request);
}


    /**
     * Links free-text values to matching active master records belonging to
     * the same tenant. Legacy text fields remain intact if no master matches.
     */
        private void syncMasterLinks(
        TenantInventoryItem item, InventoryItemRequest request
    ) {
        Long tenantId = item.getTenant().getId();

        // CATEGORY: an explicit ID must belong to this tenant, be active,
        // and match this product type.
        ProductCategory category = null;
        if (request.categoryId() != null) {
            category = categoryRepository
                .findByIdAndTenant_IdAndActiveTrue(request.categoryId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected category is not active or does not belong to this tenant"
                ));

            if (category.getProductType() == null ||
                !category.getProductType().equalsIgnoreCase(item.getProductType())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected category does not match the product type"
                );
            }
            item.setCategory(category.getName());
        } else {
            String name = item.getCategory();
            if (name != null && !name.isBlank()) {
                category = categoryRepository
                    .findByTenant_IdAndProductTypeIgnoreCaseAndActiveTrueOrderByNameAsc(
                        tenantId, item.getProductType()
                    )
                    .stream()
                    .filter(c -> c.getName() != null &&
                        c.getName().trim().equalsIgnoreCase(name.trim()))
                    .findFirst().orElse(null);
                if (category != null) item.setCategory(category.getName());
            }
        }
        item.setCategoryMaster(category);

        // GENERIC: legacy text still works, but a supplied ID is validated.
        ProductGeneric generic = null;
        if (request.genericId() != null) {
            generic = genericRepository
                .findByIdAndTenant_IdAndActiveTrue(request.genericId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected generic is not active or does not belong to this tenant"
                ));
            item.setGenericName(generic.getName());
        } else {
            String name = item.getGenericName();
            if (name != null && !name.isBlank()) {
                generic = genericRepository
                    .findByTenant_IdAndActiveTrueOrderByNameAsc(tenantId)
                    .stream()
                    .filter(g -> g.getName() != null &&
                        g.getName().trim().equalsIgnoreCase(name.trim()))
                    .findFirst().orElse(null);
                if (generic != null) item.setGenericName(generic.getName());
            }
        }
        item.setGenericMaster(generic);

        // DOSAGE FORM.
        ProductDosageForm dosage = null;
        if (request.dosageFormId() != null) {
            dosage = dosageFormRepository
                .findByIdAndTenant_IdAndActiveTrue(request.dosageFormId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected dosage form is not active or does not belong to this tenant"
                ));
            item.setDosageForm(dosage.getName());
        } else {
            String name = item.getDosageForm();
            if (name != null && !name.isBlank()) {
                dosage = dosageFormRepository
                    .findByTenant_IdAndActiveTrueOrderByNameAsc(tenantId)
                    .stream()
                    .filter(d -> d.getName() != null &&
                        d.getName().trim().equalsIgnoreCase(name.trim()))
                    .findFirst().orElse(null);
                if (dosage != null) item.setDosageForm(dosage.getName());
            }
        }
        item.setDosageFormMaster(dosage);

        // COMPANY: inactive/cross-tenant companies cannot be selected.
        ManufacturerCompany company = null;
        if (request.companyId() != null) {
            company = companyRepository
                .findByIdAndTenant_Id(request.companyId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected company does not belong to this tenant"
                ));

            if (!"Active".equalsIgnoreCase(company.getStatus())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected company is inactive"
                );
            }
            item.setBrandName(company.getName());
        } else {
            String name = item.getBrandName();
            if (name != null && !name.isBlank()) {
                company = companyRepository
                    .findAllByTenant_IdOrderByNameAsc(tenantId)
                    .stream()
                    .filter(c -> "Active".equalsIgnoreCase(c.getStatus())
                        && c.getName() != null
                        && c.getName().trim().equalsIgnoreCase(name.trim()))
                    .findFirst().orElse(null);
                if (company != null) item.setBrandName(company.getName());
            }
        }
        item.setCompanyMaster(company);
    }

        private InventoryItemResponse toResponse(TenantInventoryItem item) {
        return new InventoryItemResponse(
            item.getId(),
            item.getTenant().getId(),
            item.getName(),
            item.getGenericName(),
            item.getBrandName(),
            item.getStrength(),
            item.getDosageForm(),
            item.getCategory(),
            item.getSku(),
            item.getBarcode(),
            item.getPurchasePrice(),
            item.getSellingPrice(),
            item.getStockQuantity(),
            item.getReorderLevel(),
            item.getStatus(),
            item.getCreatedAt(),
            item.getUpdatedAt(),
            item.getDescription(),
            item.getProductType(),
            item.getCategoryMaster() == null ? null : item.getCategoryMaster().getId(),
            item.getGenericMaster() == null ? null : item.getGenericMaster().getId(),
            item.getDosageFormMaster() == null ? null : item.getDosageFormMaster().getId(),
            item.getCompanyMaster() == null ? null : item.getCompanyMaster().getId()
        );
    }
}
