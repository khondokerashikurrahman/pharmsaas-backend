package com.pharmsaas.backend.modules.inventory.service;

import com.pharmsaas.backend.modules.inventory.dto.StockRequestCreate;
import com.pharmsaas.backend.modules.inventory.dto.StockRequestDecision;
import com.pharmsaas.backend.modules.inventory.dto.StockRequestResponse;
import com.pharmsaas.backend.modules.inventory.entity.StockRequest;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import com.pharmsaas.backend.modules.inventory.repository.StockRequestRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class StockRequestService {

    private final StockRequestRepository requestRepository;
    private final TenantInventoryRepository inventoryRepository;
    private final TenantInventoryBatchRepository batchRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final InventoryMovementService movementService;

    public StockRequestService(
            StockRequestRepository requestRepository,
            TenantInventoryRepository inventoryRepository,
            TenantInventoryBatchRepository batchRepository,
            TenantMembershipRepository membershipRepository,
            TenantRepository tenantRepository,
            InventoryMovementService movementService) {
        this.requestRepository = requestRepository;
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.movementService = movementService;
    }

    private TenantMembership authorize(Long tenantId, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Tenant must be Active");
        }

        return membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Active tenant membership required"));
    }

    private boolean canApprove(MembershipRole role) {
        return role == MembershipRole.TENANT_OWNER
            || role == MembershipRole.PHARMACY_ADMIN
            || role == MembershipRole.MANAGER;
    }

    @Transactional
    public StockRequestResponse create(
            Long tenantId, Long userId, StockRequestCreate input) {
        TenantMembership membership = authorize(tenantId, userId);
        MembershipRole role = membership.getRole();

        if (role != MembershipRole.STORE_KEEPER
                && role != MembershipRole.MANAGER
                && role != MembershipRole.TENANT_OWNER
                && role != MembershipRole.PHARMACY_ADMIN) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Your role cannot request stock changes");
        }

        if (input == null || input.inventoryItemId() == null
                || input.batchId() == null
                || input.quantity() == null || input.quantity() <= 0
                || input.reason() == null || input.reason().isBlank()
                || input.reason().trim().length() > 500
                || input.movementType() == null
                || (!input.movementType().equalsIgnoreCase("STOCK_IN")
                    && !input.movementType().equalsIgnoreCase("STOCK_OUT"))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provide item ID, batch ID, positive quantity, reason and STOCK_IN or STOCK_OUT");
        }

        TenantInventoryItem item = inventoryRepository
            .findByIdAndTenant_Id(input.inventoryItemId(), tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory item not found in this tenant"));

        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Cannot request stock for an inactive item");
        }

        TenantInventoryBatch batch = batchRepository
            .findByIdAndTenant_Id(input.batchId(), tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Batch not found for this tenant"));

        if (batch.getInventoryItem() == null
                || !item.getId().equals(batch.getInventoryItem().getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Selected batch does not belong to the selected medicine");
        }

        if ("STOCK_IN".equalsIgnoreCase(input.movementType())) {
            if (batch.getExpiryDate() == null
                    || !batch.getExpiryDate().isAfter(LocalDate.now())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot request stock-in for an expired or undated batch");
            }
            if (!"ACTIVE".equalsIgnoreCase(batch.getStatus())
                    && !"NEAR_EXPIRY".equalsIgnoreCase(batch.getStatus())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot request stock-in for an inactive batch");
            }
        }

        StockRequest request = new StockRequest();
        request.setTenant(item.getTenant());
        request.setInventoryItem(item);
        request.setBatch(batch);
        request.setRequesterUserId(userId);
        request.setMovementType(input.movementType().toUpperCase());
        request.setQuantity(input.quantity());
        request.setReason(input.reason().trim());
        request.setStatus("PENDING");

        return response(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<StockRequestResponse> list(Long tenantId, Long userId) {
        TenantMembership membership = authorize(tenantId, userId);

        List<StockRequest> requests = canApprove(membership.getRole())
            ? requestRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
            : requestRepository.findAllByTenant_IdAndRequesterUserIdOrderByCreatedAtDesc(
                tenantId, userId);

        return requests.stream().map(this::response).toList();
    }

    @Transactional
    public StockRequestResponse approve(
            Long tenantId, Long requestId, Long userId,
            StockRequestDecision decision) {
        TenantMembership membership = authorize(tenantId, userId);

        if (!canApprove(membership.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Manager or tenant administrator approval required");
        }

        StockRequest request = requestRepository.findForUpdate(requestId, tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Stock request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "This request has already been decided");
        }

        if (request.getRequesterUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot approve your own stock request");
        }

        TenantInventoryItem item = inventoryRepository
            .findForUpdate(request.getInventoryItem().getId(), tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory item not found"));

        if (request.getBatch() == null || request.getBatch().getId() == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This legacy request has no batch. Reject it and create a new batch-linked request.");
        }

        TenantInventoryBatch batch = batchRepository
            .findForUpdate(request.getBatch().getId(), tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Batch not found"));

        if (batch.getInventoryItem() == null
                || !item.getId().equals(batch.getInventoryItem().getId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "The selected batch no longer belongs to this medicine");
        }

        int currentBatch = batch.getQuantity() == null ? 0 : batch.getQuantity();
        int currentInventory = item.getStockQuantity() == null
            ? 0 : item.getStockQuantity();
        int quantity = request.getQuantity();
        int newBatch;
        int newInventory;
        int signedQuantity;

        if ("STOCK_IN".equals(request.getMovementType())) {
            if (batch.getExpiryDate() == null
                    || !batch.getExpiryDate().isAfter(LocalDate.now())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot add stock to an expired or undated batch");
            }

            if (!"ACTIVE".equalsIgnoreCase(batch.getStatus())
                    && !"NEAR_EXPIRY".equalsIgnoreCase(batch.getStatus())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot add stock to an inactive batch");
            }

            try {
                newBatch = Math.addExact(currentBatch, quantity);
                newInventory = Math.addExact(currentInventory, quantity);
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Quantity exceeds the supported stock range");
            }

            signedQuantity = quantity;
        } else {
            if (quantity > currentBatch || quantity > currentInventory) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock in the selected batch or inventory");
            }

            newBatch = currentBatch - quantity;
            newInventory = currentInventory - quantity;
            signedQuantity = -quantity;
        }

        batch.setQuantity(newBatch);
        item.setStockQuantity(newInventory);

        batchRepository.save(batch);
        inventoryRepository.save(item);

        request.setStatus("APPROVED");
        request.setApproverUserId(userId);
        request.setDecidedAt(OffsetDateTime.now());
        request.setDecisionNote(cleanNote(decision));

        StockRequest savedRequest = requestRepository.save(request);

        movementService.record(
            savedRequest.getTenant(),
            item,
            batch,
            savedRequest.getMovementType(),
            signedQuantity,
            newBatch,
            "STOCK_REQUEST",
            savedRequest.getId(),
            "STOCK-REQ-" + savedRequest.getId(),
            savedRequest.getReason(),
            userId
        );

        return response(savedRequest);
    }

    @Transactional
    public StockRequestResponse reject(
            Long tenantId, Long requestId, Long userId,
            StockRequestDecision decision) {
        TenantMembership membership = authorize(tenantId, userId);

        if (!canApprove(membership.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Manager or tenant administrator decision required");
        }

        StockRequest request = requestRepository.findForUpdate(requestId, tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Stock request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "This request has already been decided");
        }

        if (request.getRequesterUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot reject your own stock request");
        }

        request.setStatus("REJECTED");
        request.setApproverUserId(userId);
        request.setDecidedAt(OffsetDateTime.now());
        request.setDecisionNote(cleanNote(decision));
        return response(requestRepository.save(request));
    }

    private String cleanNote(StockRequestDecision decision) {
        if (decision == null || decision.decisionNote() == null) {
            return null;
        }
        String note = decision.decisionNote().trim();
        if (note.length() > 500) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Decision note cannot exceed 500 characters");
        }
        return note.isBlank() ? null : note;
    }

    private StockRequestResponse response(StockRequest request) {
        TenantInventoryItem item = request.getInventoryItem();
        return new StockRequestResponse(
            request.getId(),
            request.getTenant().getId(),
            item.getId(),
            item.getName(),
            item.getSku(),
            request.getRequesterUserId(),
            request.getApproverUserId(),
            request.getMovementType(),
            request.getQuantity(),
            request.getReason(),
            request.getStatus(),
            request.getDecisionNote(),
            request.getCreatedAt(),
            request.getDecidedAt(),
            request.getBatch() == null ? null : request.getBatch().getId(),
            request.getBatch() == null ? null : request.getBatch().getBatchNumber()
        );
    }
}
