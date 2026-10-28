package com.pharmsaas.backend.modules.inventory.service;

import com.pharmsaas.backend.modules.inventory.dto.StockAdjustmentRequest;
import com.pharmsaas.backend.modules.inventory.dto.StockAdjustmentResponse;
import com.pharmsaas.backend.modules.inventory.entity.InventoryMovement;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
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

import java.util.Set;
import java.util.UUID;

@Service
public class StockAdjustmentService {

    private static final Set<MembershipRole> ALLOWED_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER
    );

    private final TenantInventoryBatchRepository batchRepository;
    private final TenantInventoryRepository inventoryRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final InventoryMovementService movementService;

    public StockAdjustmentService(
            TenantInventoryBatchRepository batchRepository,
            TenantInventoryRepository inventoryRepository,
            TenantMembershipRepository membershipRepository,
            TenantRepository tenantRepository,
            InventoryMovementService movementService) {
        this.batchRepository = batchRepository;
        this.inventoryRepository = inventoryRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.movementService = movementService;
    }

    private Tenant authorize(Long tenantId, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw error(
                HttpStatus.FORBIDDEN, "Tenant must be Active");
        }

        TenantMembership membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> error(
                HttpStatus.FORBIDDEN, "Active tenant membership required"));

        if (!ALLOWED_ROLES.contains(membership.getRole())) {
            throw error(
                HttpStatus.FORBIDDEN,
                "Your role cannot perform stock adjustments");
        }

        return tenant;
    }

    @Transactional
    public StockAdjustmentResponse adjust(
            Long tenantId,
            Long userId,
            StockAdjustmentRequest request) {

        Tenant tenant = authorize(tenantId, userId);

        if (request == null
                || request.batchId() == null
                || request.quantityChange() == null
                || request.quantityChange() == 0
                || request.reason() == null
                || request.reason().isBlank()
                || request.reason().trim().length() > 250) {
            throw error(
                HttpStatus.BAD_REQUEST,
                "Batch ID, non-zero quantity change and reason (max 250 characters) are required");
        }

        /*
         * Read the batch first to find its medicine, then lock the medicine
         * followed by the batch. This follows the item -> batch lock order
         * used by other stock-changing flows.
         */
        TenantInventoryBatch snapshot = batchRepository
            .findByIdAndTenant_Id(request.batchId(), tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Batch not found for this tenant"));

        Long itemId = snapshot.getInventoryItem().getId();

        TenantInventoryItem item = inventoryRepository
            .findForUpdate(itemId, tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Medicine not found for this tenant"));

        TenantInventoryBatch batch = batchRepository
            .findForUpdate(request.batchId(), tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Batch not found for this tenant"));

        if (batch.getInventoryItem() == null
                || !item.getId().equals(batch.getInventoryItem().getId())) {
            throw error(
                HttpStatus.CONFLICT,
                "Batch-to-medicine association changed; retry the adjustment");
        }

        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
            throw error(
                HttpStatus.CONFLICT,
                "Cannot adjust stock for an inactive medicine");
        }

        if (request.quantityChange() > 0) {
            if (batch.getExpiryDate() == null
                    || !batch.getExpiryDate().isAfter(java.time.LocalDate.now())) {
                throw error(
                    HttpStatus.CONFLICT,
                    "Cannot add stock to an expired or undated batch");
            }

            String batchStatus = batch.getStatus() == null
                ? "" : batch.getStatus();

            if (!"ACTIVE".equalsIgnoreCase(batchStatus)
                    && !"NEAR_EXPIRY".equalsIgnoreCase(batchStatus)) {
                throw error(
                    HttpStatus.CONFLICT,
                    "Cannot add stock to an inactive batch");
            }
        }

        int currentBatch = batch.getQuantity() == null
            ? 0 : batch.getQuantity();
        int currentInventory = item.getStockQuantity() == null
            ? 0 : item.getStockQuantity();
        int change = request.quantityChange();

        final int newBatch;
        final int newInventory;

        try {
            newBatch = Math.addExact(currentBatch, change);
            newInventory = Math.addExact(currentInventory, change);
        } catch (ArithmeticException exception) {
            throw error(
                HttpStatus.BAD_REQUEST, "Quantity is outside the supported range");
        }

        if (newBatch < 0 || newInventory < 0) {
            throw error(
                HttpStatus.CONFLICT,
                "Adjustment would make batch or inventory stock negative");
        }

        batch.setQuantity(newBatch);
        item.setStockQuantity(newInventory);

        batchRepository.save(batch);
        inventoryRepository.save(item);

        String adjustmentNumber = "ADJ-"
            + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        InventoryMovement movement = movementService.record(
            tenant,
            item,
            batch,
            "ADJUSTMENT",
            change,
            newBatch,
            "STOCK_ADJUSTMENT",
            null,
            adjustmentNumber,
            request.reason().trim(),
            userId
        );

        return new StockAdjustmentResponse(
            movement.getId(),
            adjustmentNumber,
            tenantId,
            item.getId(),
            item.getName(),
            batch.getId(),
            batch.getBatchNumber(),
            change,
            newBatch,
            newInventory,
            request.reason().trim(),
            userId,
            movement.getCreatedAt()
        );
    }

    private ResponseStatusException error(
            HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
