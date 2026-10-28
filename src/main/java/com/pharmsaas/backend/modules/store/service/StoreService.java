package com.pharmsaas.backend.modules.store.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.store.dto.StoreRequest;
import com.pharmsaas.backend.modules.store.dto.StoreResponse;
import com.pharmsaas.backend.modules.store.entity.Store;
import com.pharmsaas.backend.modules.store.repository.StoreRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StoreService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("Active", "Inactive");

    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public StoreService(
            StoreRepository storeRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository) {

        this.storeRepository = storeRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    /*
     * Store permission policy:
     *
     * TENANT_OWNER / PHARMACY_ADMIN:
     *   Create, Read, Update, Delete
     *
     * MANAGER / STORE_KEEPER:
     *   Read only
     *
     * SUPER_ADMIN and other operational roles:
     *   No access to tenant store management through this API.
     *
     * A valid active membership in the specified tenant is mandatory.
     */
    private Tenant requireTenantAccess(
            Long tenantId,
            Long userId,
            boolean writeOperation) {

        if (tenantId == null || tenantId <= 0
                || userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid tenant and user IDs are required."
            );
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant not found."
                ));

        if (tenant.getStatus() == null
                || !"Active".equalsIgnoreCase(
                        tenant.getStatus().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This pharmacy is not active."
            );
        }

        TenantMembership membership = membershipRepository
                .findByTenant_IdAndUser_IdAndActiveTrue(
                        tenantId,
                        userId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You do not have an active membership in this pharmacy."
                ));

        MembershipRole role = membership.getRole();

        if (role == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your membership does not have a valid role."
            );
        }

        boolean isTenantAdmin =
                role == MembershipRole.TENANT_OWNER
                || role == MembershipRole.PHARMACY_ADMIN;

        boolean isReadOnlyOperator =
                role == MembershipRole.MANAGER
                || role == MembershipRole.STORE_KEEPER
                || role == MembershipRole.PHARMACIST
                || role == MembershipRole.STAFF
                || role == MembershipRole.CASHIER;

        if (isTenantAdmin) {
            return tenant;
        }

        if (!writeOperation && isReadOnlyOperator) {
            return tenant;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                writeOperation
                        ? "Only the Tenant Owner or Pharmacy Admin can modify stores."
                        : "Your role cannot access store management."
        );
    }

    /**
     * Creates the default store for a tenant when it has no stores.
     * Existing stores are preserved.
     */
    @Transactional
    public Store ensureDefaultStore(Tenant tenant) {
        if (tenant == null || tenant.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A saved tenant is required to create a default store."
            );
        }

        List<Store> existingStores = storeRepository.findByTenant_Id(tenant.getId());
        if (!existingStores.isEmpty()) {
            return existingStores.get(0);
        }

        Store store = new Store();
        store.setTenant(tenant);
        store.setStoreCode("MAIN");
        store.setName("Main Store");
        store.setStatus("Active");

        return storeRepository.save(store);
    }

    @Transactional
    public StoreResponse create(
            Long tenantId,
            Long userId,
            StoreRequest request) {

        Tenant tenant = requireTenantAccess(
                tenantId,
                userId,
                true
        );

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Store request is required."
            );
        }

        String code = normalizeCode(request.storeCode());

        if (storeRepository.existsByTenant_IdAndStoreCode(
                tenantId,
                code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Store code already exists for this tenant."
            );
        }

        Store store = new Store();
        store.setTenant(tenant);
        store.setStoreCode(code);

        applyRequest(store, request, true);

        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public List<StoreResponse> getAll(
            Long tenantId,
            Long userId) {

        Tenant tenant = requireTenantAccess(
                tenantId,
                userId,
                false
        );

        List<Store> stores = storeRepository.findByTenant_Id(tenantId);

        if (stores.isEmpty()) {
            stores = List.of(ensureDefaultStore(tenant));
        }

        return stores.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse getById(
            Long tenantId,
            Long storeId,
            Long userId) {

        requireTenantAccess(
                tenantId,
                userId,
                false
        );

        return toResponse(findStore(tenantId, storeId));
    }

    @Transactional
    public StoreResponse update(
            Long tenantId,
            Long storeId,
            Long userId,
            StoreRequest request) {

        requireTenantAccess(
                tenantId,
                userId,
                true
        );

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Store request is required."
            );
        }

        Store store = findStore(tenantId, storeId);
        String code = normalizeCode(request.storeCode());

        if (storeRepository.existsByTenant_IdAndStoreCodeAndIdNot(
                tenantId,
                code,
                storeId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Store code already exists for this tenant."
            );
        }

        store.setStoreCode(code);
        applyRequest(store, request, false);

        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public void delete(
            Long tenantId,
            Long storeId,
            Long userId) {

        requireTenantAccess(
                tenantId,
                userId,
                true
        );

        Store store = findStore(tenantId, storeId);
        storeRepository.delete(store);
    }

    private Store findStore(
            Long tenantId,
            Long storeId) {

        if (storeId == null || storeId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid store ID is required."
            );
        }

        return storeRepository.findByIdAndTenant_Id(
                        storeId,
                        tenantId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Store not found for this tenant."
                ));
    }

    private void applyRequest(
            Store store,
            StoreRequest request,
            boolean creating) {

        if (request.name() == null
                || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Store name is required."
            );
        }

        store.setName(request.name().trim());
        store.setPhone(request.phone());
        store.setEmail(request.email());
        store.setAddress(request.address());
        store.setCity(request.city());
        store.setDistrict(request.district());

        String status = request.status();

        if (status == null || status.isBlank()) {
            if (creating) {
                status = "Active";
            } else {
                return;
            }
        }

        String normalizedStatus = status.trim();

        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid store status. Allowed values: "
                            + ALLOWED_STATUSES
            );
        }

        store.setStatus(normalizedStatus);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Store code is required."
            );
        }

        return code.trim().toUpperCase(Locale.ROOT);
    }

    private StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getTenant().getId(),
                store.getStoreCode(),
                store.getName(),
                store.getPhone(),
                store.getEmail(),
                store.getAddress(),
                store.getCity(),
                store.getDistrict(),
                store.getStatus(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}
