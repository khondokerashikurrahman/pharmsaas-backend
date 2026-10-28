package com.pharmsaas.backend.modules.inventory.service;

import com.pharmsaas.backend.modules.inventory.dto.InventoryMovementResponse;
import com.pharmsaas.backend.modules.inventory.entity.InventoryMovement;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import com.pharmsaas.backend.modules.inventory.repository.InventoryMovementRepository;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.store.entity.Store;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class InventoryMovementService {

    private static final Set<String> READ_ROLES = Set.of(
        "TENANT_OWNER",
        "PHARMACY_ADMIN",
        "MANAGER",
        "STORE_KEEPER",
        "PHARMACIST",
        "STAFF",
        "CASHIER"
    );

    private final InventoryMovementRepository movementRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public InventoryMovementService(
        InventoryMovementRepository movementRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository
    ) {
        this.movementRepository = movementRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public InventoryMovement record(
        Tenant tenant,
        TenantInventoryItem item,
        TenantInventoryBatch batch,
        String movementType,
        Integer quantityChange,
        Integer quantityAfter,
        String referenceType,
        Long referenceId,
        String referenceNumber,
        String reason,
        Long actorUserId
    ) {
        return record(
            tenant, item, batch, movementType, quantityChange, quantityAfter,
            referenceType, referenceId, referenceNumber, reason, actorUserId, null
        );
    }

    @Transactional
    public InventoryMovement record(
        Tenant tenant,
        TenantInventoryItem item,
        TenantInventoryBatch batch,
        String movementType,
        Integer quantityChange,
        Integer quantityAfter,
        String referenceType,
        Long referenceId,
        String referenceNumber,
        String reason,
        Long actorUserId,
        Store store
    ) {
        if (tenant == null || tenant.getId() == null ||
            item == null || item.getId() == null ||
            item.getTenant() == null ||
            !tenant.getId().equals(item.getTenant().getId())) {
            throw new IllegalArgumentException(
                "Inventory movement must reference a product belonging to its tenant"
            );
        }

        if (batch != null &&
            (batch.getTenant() == null ||
             !tenant.getId().equals(batch.getTenant().getId()) ||
             batch.getInventoryItem() == null ||
             !item.getId().equals(batch.getInventoryItem().getId()))) {
            throw new IllegalArgumentException(
                "Inventory movement batch does not belong to the specified product and tenant"
            );
        }

        if (store != null &&
            (store.getTenant() == null ||
             store.getTenant().getId() == null ||
             !tenant.getId().equals(store.getTenant().getId()))) {
            throw new IllegalArgumentException(
                "Inventory movement store must belong to the specified tenant"
            );
        }

        if (movementType == null || movementType.isBlank() ||
            quantityChange == null || quantityChange == 0) {
            throw new IllegalArgumentException(
                "Movement type and non-zero quantity change are required"
            );
        }

        InventoryMovement movement = new InventoryMovement();
        movement.setTenant(tenant);
        movement.setStore(store);
        movement.setInventoryItem(item);
        movement.setBatch(batch);
        movement.setMovementType(
            movementType.trim().toUpperCase(Locale.ROOT)
        );
        movement.setQuantityChange(quantityChange);
        movement.setQuantityAfter(quantityAfter);
        movement.setReferenceType(clean(referenceType, 40));
        movement.setReferenceId(referenceId);
        movement.setReferenceNumber(clean(referenceNumber, 100));
        movement.setReason(clean(reason, 250));
        movement.setActorUserId(actorUserId);

        // Uses the caller's transaction when invoked within a transactional service.
        return movementRepository.save(movement);
    }

    @Transactional
    public List<InventoryMovementResponse> list(Long tenantId, Long userId) {
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

        if (!READ_ROLES.contains(membership.getRole().name())) {
            throw error(HttpStatus.FORBIDDEN, "You cannot view inventory movements");
        }

        return movementRepository
            .findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private InventoryMovementResponse toResponse(InventoryMovement movement) {
        TenantInventoryItem item = movement.getInventoryItem();
        TenantInventoryBatch batch = movement.getBatch();

        return new InventoryMovementResponse(
            movement.getId(),
            item == null ? null : item.getId(),
            item == null ? null : item.getName(),
            batch == null ? null : batch.getId(),
            batch == null ? null : batch.getBatchNumber(),
            movement.getMovementType(),
            movement.getQuantityChange(),
            movement.getQuantityAfter(),
            movement.getReferenceType(),
            movement.getReferenceId(),
            movement.getReferenceNumber(),
            movement.getReason(),
            movement.getActorUserId(),
            movement.getCreatedAt(),
            movement.getStore() == null ? null : movement.getStore().getId(),
            movement.getStore() == null ? null : movement.getStore().getName()
        );
    }

    private String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        return result.length() > maxLength
            ? result.substring(0, maxLength)
            : result;
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
