package com.pharmsaas.backend.modules.grn.service;

import com.pharmsaas.backend.modules.grn.dto.GrnCreateRequest;
import com.pharmsaas.backend.modules.grn.dto.GrnResponse;
import com.pharmsaas.backend.modules.grn.entity.GoodsReceivedNote;
import com.pharmsaas.backend.modules.grn.entity.GoodsReceivedNoteItem;
import com.pharmsaas.backend.modules.grn.repository.GoodsReceivedNoteRepository;
import com.pharmsaas.backend.modules.inventory.dto.InventoryBatchCreateRequest;
import com.pharmsaas.backend.modules.inventory.dto.InventoryBatchResponse;
import com.pharmsaas.backend.modules.inventory.service.InventoryBatchService;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GrnService {

    private final GoodsReceivedNoteRepository grnRepository;
    private final InventoryBatchService batchService;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public GrnService(
            GoodsReceivedNoteRepository grnRepository,
            InventoryBatchService batchService,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository) {
        this.grnRepository = grnRepository;
        this.batchService = batchService;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    private void authorize(Long tenantId, Long userId, boolean write) {
        var tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null || !"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant must be Active");
        }

        var membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Active tenant membership required"));

        MembershipRole role = membership.getRole();
        boolean allowed = role == MembershipRole.TENANT_OWNER
            || role == MembershipRole.PHARMACY_ADMIN
            || role == MembershipRole.MANAGER
            || (!write && role == MembershipRole.STORE_KEEPER);

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access GRN records");
        }
    }

    @Transactional
    public GrnResponse create(Long tenantId, Long userId, GrnCreateRequest request) {
        authorize(tenantId, userId, true);

        if (request == null) badRequest("GRN request is required");
        String supplier = clean(request.supplierName());
        String po = clean(request.purchaseOrderNumber());
        String invoice = clean(request.supplierInvoiceNumber());

        if (supplier.isBlank()) badRequest("Supplier name is required");
        if (po.isBlank()) badRequest("Purchase order number is required");
        if (invoice.isBlank()) badRequest("Supplier invoice number is required");
        if (request.receivedDate() == null) badRequest("Received date is required");
        if (request.receivedDate().isAfter(LocalDate.now())) badRequest("Received date cannot be in the future");
        if (request.items() == null || request.items().isEmpty()) badRequest("At least one GRN item is required");

        if (grnRepository.existsByTenant_IdAndSupplierInvoiceNumberIgnoreCase(tenantId, invoice)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This supplier invoice is already recorded for this tenant");
        }

        var tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        String grnNumber = "GRN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        GoodsReceivedNote grn = new GoodsReceivedNote();
        grn.setTenant(tenant);
        grn.setGrnNumber(grnNumber);
        grn.setSupplierName(supplier);
        grn.setPurchaseOrderNumber(po);
        grn.setSupplierInvoiceNumber(invoice);
        grn.setReceivedDate(request.receivedDate());
        grn.setNotes(clean(request.notes()));
        grn.setStatus("RECEIVED");

        // Persist header first. Any later exception rolls it back with the stock changes.
        grnRepository.saveAndFlush(grn);

        for (GrnCreateRequest.Item line : request.items()) {
            validateItem(line);

            InventoryBatchResponse batch = batchService.create(
                tenantId,
                userId,
                new InventoryBatchCreateRequest(
                    line.inventoryItemId(),
                    clean(line.batchNumber()),
                    line.manufactureDate(),
                    line.expiryDate(),
                    line.quantity(),
                    line.purchasePrice(),
                    line.sellingPrice()
                )
            );

            GoodsReceivedNoteItem savedLine = new GoodsReceivedNoteItem();
            savedLine.setInventoryItemId(batch.inventoryItemId());
            savedLine.setMedicineName(batch.medicineName());
            savedLine.setInventoryBatchId(batch.id());
            savedLine.setBatchNumber(batch.batchNumber());
            savedLine.setManufactureDate(batch.manufactureDate());
            savedLine.setExpiryDate(batch.expiryDate());
            savedLine.setQuantity(batch.quantity());
            savedLine.setPurchasePrice(line.purchasePrice());
            savedLine.setSellingPrice(line.sellingPrice());
            grn.addItem(savedLine);
        }

        grnRepository.save(grn);
        return toResponse(grn);
    }

    @Transactional(readOnly = true)
    public List<GrnResponse> list(Long tenantId, Long userId) {
        authorize(tenantId, userId, false);
        return grnRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GrnResponse get(Long tenantId, Long userId, Long grnId) {
        authorize(tenantId, userId, false);
        GoodsReceivedNote grn = grnRepository.findByIdAndTenant_Id(grnId, tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GRN not found"));
        return toResponse(grn);
    }

    private void validateItem(GrnCreateRequest.Item item) {
        if (item == null || item.inventoryItemId() == null) badRequest("Medicine is required for every GRN item");
        if (clean(item.batchNumber()).isBlank()) badRequest("Batch number is required");
        if (item.quantity() == null || item.quantity() <= 0) badRequest("Quantity must be greater than zero");
        if (item.purchasePrice() == null || item.purchasePrice().compareTo(BigDecimal.ZERO) <= 0) badRequest("Purchase price must be greater than zero");
        if (item.sellingPrice() == null || item.sellingPrice().compareTo(BigDecimal.ZERO) <= 0) badRequest("Selling price must be greater than zero");
        if (item.manufactureDate() == null) badRequest("Manufacturing date is required");
        if (item.expiryDate() == null || !item.expiryDate().isAfter(LocalDate.now())) badRequest("Expiry date must be in the future");
        if (item.manufactureDate() != null && item.expiryDate() != null
                && !item.expiryDate().isAfter(item.manufactureDate())) {
            badRequest("Expiry date must be after manufacturing date");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private GrnResponse toResponse(GoodsReceivedNote grn) {
        List<GrnResponse.Item> items = grn.getItems().stream().map(item ->
            new GrnResponse.Item(
                item.getId(),
                item.getInventoryItemId(),
                item.getMedicineName(),
                item.getInventoryBatchId(),
                item.getBatchNumber(),
                item.getManufactureDate(),
                item.getExpiryDate(),
                item.getQuantity(),
                item.getPurchasePrice(),
                item.getSellingPrice(),
                item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            )
        ).toList();

        return new GrnResponse(
            grn.getId(),
            grn.getTenant().getId(),
            grn.getGrnNumber(),
            grn.getSupplierName(),
            grn.getPurchaseOrderNumber(),
            grn.getSupplierInvoiceNumber(),
            grn.getReceivedDate(),
            grn.getNotes(),
            grn.getStatus(),
            grn.getCreatedAt(),
            items
        );
    }
}
