package com.pharmsaas.backend.modules.salesreturn.service;

import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.entity.StoreInventoryStock;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
import com.pharmsaas.backend.modules.inventory.repository.StoreInventoryStockRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryRepository;
import com.pharmsaas.backend.modules.inventory.service.InventoryMovementService;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.salesreturn.dto.SaleReturnCreateRequest;
import com.pharmsaas.backend.modules.salesreturn.dto.SaleReturnResponse;
import com.pharmsaas.backend.modules.sale.entity.Sale;
import com.pharmsaas.backend.modules.sale.entity.SaleItem;
import com.pharmsaas.backend.modules.salesreturn.entity.SaleReturn;
import com.pharmsaas.backend.modules.salesreturn.entity.SaleReturnItem;
import com.pharmsaas.backend.modules.sale.repository.SaleItemRepository;
import com.pharmsaas.backend.modules.sale.repository.SaleRepository;
import com.pharmsaas.backend.modules.salesreturn.repository.SaleReturnItemRepository;
import com.pharmsaas.backend.modules.salesreturn.repository.SaleReturnRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.user.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class SalesReturnService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleReturnRepository returnRepository;
    private final SaleReturnItemRepository returnItemRepository;
    private final TenantInventoryBatchRepository batchRepository;
    private final TenantInventoryRepository inventoryRepository;
    private final StoreInventoryStockRepository storeStockRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final InventoryMovementService movementService;

    public SalesReturnService(
        SaleRepository saleRepository,
        SaleItemRepository saleItemRepository,
        SaleReturnRepository returnRepository,
        SaleReturnItemRepository returnItemRepository,
        TenantInventoryBatchRepository batchRepository,
        TenantInventoryRepository inventoryRepository,
        StoreInventoryStockRepository storeStockRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository,
        InventoryMovementService movementService
    ) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.returnRepository = returnRepository;
        this.returnItemRepository = returnItemRepository;
        this.batchRepository = batchRepository;
        this.inventoryRepository = inventoryRepository;
        this.storeStockRepository = storeStockRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.movementService = movementService;
    }

    @Transactional
    public SaleReturnResponse create(
        Long tenantId,
        Long saleId,
        Long userId,
        SaleReturnCreateRequest request
    ) {
        if (request == null
            || request.requestKey() == null
            || request.requestKey().isBlank()
            || request.requestKey().length() > 100) {
            throw error(HttpStatus.BAD_REQUEST, "A valid refund request key is required");
        }

        if (request.reason() == null || request.reason().isBlank()
            || request.reason().trim().length() > 250) {
            throw error(HttpStatus.BAD_REQUEST, "A return reason is required (max 250 characters)");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "Select at least one item to return");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null || !tenant.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw error(HttpStatus.FORBIDDEN, "Tenant is not active");
        }

        var membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Active tenant membership required"));

        String role = membership.getRole().name();
        if (!Set.of(
            "TENANT_OWNER", "PHARMACY_ADMIN", "MANAGER",
            "PHARMACIST", "CASHIER"
        ).contains(role)) {
            throw error(HttpStatus.FORBIDDEN, "Your role cannot process sale returns");
        }

        // The parent sale lock serializes returns for the same sale.
        Sale sale = saleRepository.findForUpdateByIdAndTenant_Id(saleId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Sale not found for this tenant"));

        // Retry-safe: the same tenant/request key returns the previously committed result.
        Optional<SaleReturn> existing = returnRepository
            .findByTenant_IdAndRequestKey(tenantId, request.requestKey().trim());

        if (existing.isPresent()) {
            if (!existing.get().getSale().getId().equals(saleId)) {
                throw error(HttpStatus.CONFLICT, "This request key was already used for another sale");
            }
            return toResponse(existing.get());
        }

        String saleStatus = sale.getStatus() == null ? "" : sale.getStatus().toUpperCase(Locale.ROOT);
        if (!Set.of("COMPLETED", "PARTIALLY_REFUNDED").contains(saleStatus)) {
            throw error(HttpStatus.CONFLICT, "This sale is not returnable");
        }

        List<SaleItem> soldItems =
            saleItemRepository.findAllBySale_IdOrderByIdAsc(saleId);

        if (soldItems.isEmpty()) {
            throw error(HttpStatus.CONFLICT, "No sale items were found");
        }

        Map<Long, BigDecimal> originalNetLineAmounts = allocateNetLineAmounts(sale, soldItems);
        Map<Long, BigDecimal> originalNetLineBases = allocateNetLineBaseAmounts(sale, soldItems);
        Map<Long, Integer> requestedQuantityByLine = new HashMap<>();
        Set<Long> requestBatchIds = new HashSet<>();
        List<PreparedLine> prepared = new ArrayList<>();
        BigDecimal refundTotal = ZERO;

        for (SaleReturnCreateRequest.ReturnLineRequest line : request.items()) {
            if (line == null || line.batchId() == null || line.batchId() <= 0
                || line.quantity() == null || line.quantity() <= 0) {
                throw error(HttpStatus.BAD_REQUEST, "Each return item needs a batch ID and positive quantity");
            }

            if (!requestBatchIds.add(line.batchId())) {
                throw error(HttpStatus.BAD_REQUEST, "Duplicate batch in return request");
            }

            SaleItem soldLine = soldItems.stream()
                .filter(item -> item.getBatch() != null
                    && item.getBatch().getId().equals(line.batchId()))
                .findFirst()
                .orElseThrow(() -> error(
                    HttpStatus.BAD_REQUEST, "This batch was not part of the original sale"
                ));

            List<SaleReturnItem> previousReturns =
                returnItemRepository.findAllBySaleItem_Id(soldLine.getId());

            int alreadyReturned = previousReturns.stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

            BigDecimal alreadyRefundedAmount = previousReturns.stream()
                .map(item -> money(item.getRefundAmount()))
                .reduce(ZERO, BigDecimal::add);

            int soldQuantity = soldLine.getQuantity() == null ? 0 : soldLine.getQuantity();
            int remaining = soldQuantity - alreadyReturned;

            if (line.quantity() > remaining) {
                throw error(
                    HttpStatus.CONFLICT,
                    "Return quantity exceeds the unreturned quantity for batch " + line.batchId()
                );
            }

            TenantInventoryBatch batch = batchRepository
                .findForUpdate(line.batchId(), tenantId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Batch not found for this tenant"));

            TenantInventoryItem inventoryItem = inventoryRepository
                .findForUpdate(soldLine.getInventoryItem().getId(), tenantId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Inventory product not found"));

            if (batch.getInventoryItem() == null
                || !batch.getInventoryItem().getId().equals(inventoryItem.getId())
                || !soldLine.getBatch().getId().equals(batch.getId())) {
                throw error(HttpStatus.CONFLICT, "The sold product and batch no longer match");
            }

            BigDecimal fullLineRefund = originalNetLineAmounts.getOrDefault(
                soldLine.getId(), money(soldLine.getSubtotal())
            );

            boolean completingLine = line.quantity() == remaining;
            BigDecimal refundAmount = completingLine
                ? fullLineRefund.subtract(alreadyRefundedAmount)
                : fullLineRefund.multiply(BigDecimal.valueOf(line.quantity()))
                    .divide(BigDecimal.valueOf(soldQuantity), 2, RoundingMode.HALF_UP);

            refundAmount = money(refundAmount);

            BigDecimal fullLineCommissionBase = originalNetLineBases.getOrDefault(
                soldLine.getId(), ZERO
            );
            BigDecimal commissionRate = money(sale.getCommissionRateSnapshot());
            BigDecimal fullLineCommission = fullLineCommissionBase
                .multiply(commissionRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            BigDecimal alreadyReversedCommission = previousReturns.stream()
                .map(SaleReturnItem::getCommissionReversedAmount)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add);

            BigDecimal commissionReversal = completingLine
                ? fullLineCommission.subtract(alreadyReversedCommission)
                : fullLineCommission.multiply(BigDecimal.valueOf(line.quantity()))
                    .divide(BigDecimal.valueOf(soldQuantity), 2, RoundingMode.HALF_UP);

            commissionReversal = money(commissionReversal);

            prepared.add(new PreparedLine(
                soldLine, batch, inventoryItem, line.quantity(),
                refundAmount, commissionReversal
            ));

            requestedQuantityByLine.put(soldLine.getId(), line.quantity());
            refundTotal = refundTotal.add(refundAmount);
        }

        User currentUser = membership.getUser();

        SaleReturn refund = new SaleReturn();
        refund.setTenant(tenant);
        refund.setSale(sale);
        refund.setCreatedBy(currentUser);
        refund.setRequestKey(request.requestKey().trim());
        refund.setRefundNumber(
            "RET-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT)
        );
        refund.setReason(request.reason().trim());
        refund.setRefundAmount(money(refundTotal));
        refund.setStatus("COMPLETED");
        returnRepository.saveAndFlush(refund);

        List<SaleReturnItem> returnLines = new ArrayList<>();

        for (PreparedLine line : prepared) {
            TenantInventoryBatch batch = line.batch();
            TenantInventoryItem inventoryItem = line.inventoryItem();

            // Returned goods remain outside sellable stock until an authorised
            // operator inspects them and explicitly releases them.
            SaleReturnItem returnItem = new SaleReturnItem();
            returnItem.setSaleReturn(refund);
            returnItem.setSaleItem(line.saleItem());
            returnItem.setBatch(batch);
            returnItem.setQuantity(line.quantity());
            returnItem.setRefundAmount(line.refundAmount());
            returnItem.setCommissionReversedAmount(line.commissionReversedAmount());
            returnItem.setStockDisposition("QUARANTINED");
            returnLines.add(returnItem);
        }

        returnItemRepository.saveAll(returnLines);

        boolean fullyReturned = true;

        for (SaleItem item : soldItems) {
            int oldQuantity = returnItemRepository.findAllBySaleItem_Id(item.getId())
                .stream()
                .mapToInt(returnItem -> returnItem.getQuantity() == null ? 0 : returnItem.getQuantity())
                .sum();

            int soldQuantity = item.getQuantity() == null ? 0 : item.getQuantity();

            if (oldQuantity < soldQuantity) {
                fullyReturned = false;
                break;
            }
        }

        sale.setStatus(fullyReturned ? "REFUNDED" : "PARTIALLY_REFUNDED");
        saleRepository.save(sale);

        return toResponse(refund);
    }

    @Transactional
    public List<SaleReturnResponse> listReturns(Long tenantId, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null || !tenant.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw error(HttpStatus.FORBIDDEN, "Tenant is not active");
        }

        var membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Active tenant membership required"));

        String role = membership.getRole().name();
        if (!Set.of(
            "TENANT_OWNER", "PHARMACY_ADMIN", "MANAGER",
            "STORE_KEEPER", "PHARMACIST", "CASHIER"
        ).contains(role)) {
            throw error(HttpStatus.FORBIDDEN, "Your role cannot view sale returns");
        }

        return returnRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SaleReturnResponse restockReturnedItem(
        Long tenantId,
        Long returnId,
        Long returnItemId,
        Long userId
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null || !tenant.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw error(HttpStatus.FORBIDDEN, "Tenant is not active");
        }

        var membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Active tenant membership required"));

        String role = membership.getRole().name();
        if (!Set.of("TENANT_OWNER", "PHARMACY_ADMIN", "MANAGER", "STORE_KEEPER").contains(role)) {
            throw error(HttpStatus.FORBIDDEN, "Your role cannot release returned stock");
        }

        SaleReturnItem returnItem = returnItemRepository
            .findForUpdateByIdAndReturnAndTenant(returnItemId, returnId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Returned item not found"));

        // Make repeated requests safe: never increase stock twice.
        if ("RESTOCKED".equalsIgnoreCase(returnItem.getStockDisposition())) {
            return toResponse(returnItem.getSaleReturn());
        }

        if (!"QUARANTINED".equalsIgnoreCase(returnItem.getStockDisposition())) {
            throw error(HttpStatus.CONFLICT, "Returned item has an unknown stock disposition");
        }

        Long batchId = returnItem.getBatch().getId();
        Long productId = returnItem.getSaleItem().getInventoryItem().getId();

        TenantInventoryItem inventoryItem = inventoryRepository.findForUpdate(productId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Inventory product not found"));

        TenantInventoryBatch batch = batchRepository.findForUpdate(batchId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Inventory batch not found"));

        if (batch.getInventoryItem() == null
            || !batch.getInventoryItem().getId().equals(inventoryItem.getId())) {
            throw error(HttpStatus.CONFLICT, "Returned batch does not match the original product");
        }

        if (batch.getExpiryDate() == null
            || !batch.getExpiryDate().isAfter(LocalDate.now())) {
            throw error(HttpStatus.CONFLICT, "Expired or undated stock cannot be released for sale");
        }

        if (inventoryItem.getStatus() == null
            || !inventoryItem.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw error(HttpStatus.CONFLICT, "Inactive products cannot be released for sale");
        }

        int quantity = returnItem.getQuantity() == null ? 0 : returnItem.getQuantity();
        if (quantity <= 0) {
            throw error(HttpStatus.CONFLICT, "Returned quantity is invalid");
        }

        var originalSale = returnItem.getSaleReturn().getSale();
        var originalStore = originalSale.getStore();
        if (originalStore == null) {
            throw error(
                HttpStatus.CONFLICT,
                "The original sale has no store; returned stock cannot be released safely"
            );
        }

        StoreInventoryStock storeStock = storeStockRepository
            .findForUpdate(tenantId, originalStore.getId(), batchId)
            .orElse(null);

        if (storeStock == null) {
            storeStock = new StoreInventoryStock();
            storeStock.setTenant(tenant);
            storeStock.setStore(originalStore);
            storeStock.setInventoryItem(inventoryItem);
            storeStock.setBatch(batch);
            storeStock.setQuantity(0);
        }

        int currentStoreQuantity =
            storeStock.getQuantity() == null ? 0 : storeStock.getQuantity();

        int nextStoreQuantity;
        try {
            nextStoreQuantity = Math.addExact(currentStoreQuantity, quantity);
        } catch (ArithmeticException ex) {
            throw error(HttpStatus.CONFLICT, "Returned stock quantity exceeds the supported limit");
        }

        batch.setQuantity((batch.getQuantity() == null ? 0 : batch.getQuantity()) + quantity);
        inventoryItem.setStockQuantity(
            (inventoryItem.getStockQuantity() == null ? 0 : inventoryItem.getStockQuantity()) + quantity
        );

        batchRepository.save(batch);
        inventoryRepository.save(inventoryItem);
        storeStock.setQuantity(nextStoreQuantity);
        storeStockRepository.save(storeStock);

        returnItem.setStockDisposition("RESTOCKED");
        returnItemRepository.save(returnItem);

        // Record only after quarantine is explicitly released to sellable stock.
        movementService.record(
            tenant,
            inventoryItem,
            batch,
            "RETURN_RESTOCKED",
            quantity,
            storeStock.getQuantity(),
            "SALE_RETURN",
            returnItem.getSaleReturn().getId(),
            returnItem.getSaleReturn().getRefundNumber(),
            returnItem.getSaleReturn().getReason(),
            userId,
            originalStore
        );

        return toResponse(returnItem.getSaleReturn());
    }

    private Map<Long, BigDecimal> allocateNetLineAmounts(Sale sale, List<SaleItem> items) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();

        BigDecimal totalDiscount = money(sale.getDiscount());
        BigDecimal totalTax = money(sale.getTax());

        // SaleItem.subtotal already excludes the individual line discount.
        // Only the remaining order-level discount should be allocated here.
        BigDecimal itemDiscountTotal = items.stream()
            .map(item -> money(item.getDiscountAmount()))
            .reduce(ZERO, BigDecimal::add);

        BigDecimal orderDiscount = totalDiscount.subtract(itemDiscountTotal);
        if (orderDiscount.signum() < 0) {
            orderDiscount = ZERO;
        }

        BigDecimal afterItemDiscounts = items.stream()
            .map(item -> money(item.getSubtotal()))
            .reduce(ZERO, BigDecimal::add);

        BigDecimal taxableTotal = afterItemDiscounts.subtract(orderDiscount);

        BigDecimal allocatedOrderDiscount = ZERO;
        BigDecimal allocatedTax = ZERO;

        for (int i = 0; i < items.size(); i++) {
            SaleItem item = items.get(i);
            BigDecimal lineSubtotal = money(item.getSubtotal());
            boolean last = i == items.size() - 1;

            BigDecimal orderDiscountShare;
            if (last) {
                // Put rounding remainder on the last line so totals reconcile.
                orderDiscountShare = orderDiscount.subtract(allocatedOrderDiscount);
            } else if (afterItemDiscounts.signum() > 0) {
                orderDiscountShare = orderDiscount.multiply(lineSubtotal)
                    .divide(afterItemDiscounts, 2, RoundingMode.HALF_UP);
            } else {
                orderDiscountShare = ZERO;
            }
            allocatedOrderDiscount = allocatedOrderDiscount.add(orderDiscountShare);

            BigDecimal lineTaxable = lineSubtotal.subtract(orderDiscountShare);
            if (lineTaxable.signum() < 0) {
                lineTaxable = ZERO;
            }

            BigDecimal taxShare;
            if (last) {
                taxShare = totalTax.subtract(allocatedTax);
            } else if (taxableTotal.signum() > 0) {
                taxShare = totalTax.multiply(lineTaxable)
                    .divide(taxableTotal, 2, RoundingMode.HALF_UP);
            } else {
                taxShare = ZERO;
            }
            allocatedTax = allocatedTax.add(taxShare);

            // Return amount is the line amount after both discounts, plus its tax share.
            result.put(item.getId(), money(lineTaxable.add(taxShare)));
        }

        return result;
    }

    private Map<Long, BigDecimal> allocateNetLineBaseAmounts(
        Sale sale, List<SaleItem> items
    ) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();

        BigDecimal totalDiscount = money(sale.getDiscount());
        BigDecimal itemDiscountTotal = items.stream()
            .map(item -> money(item.getDiscountAmount()))
            .reduce(ZERO, BigDecimal::add);

        BigDecimal orderDiscount = totalDiscount.subtract(itemDiscountTotal);
        if (orderDiscount.signum() < 0) {
            orderDiscount = ZERO;
        }

        BigDecimal afterItemDiscounts = items.stream()
            .map(item -> money(item.getSubtotal()))
            .reduce(ZERO, BigDecimal::add);

        BigDecimal allocatedOrderDiscount = ZERO;

        for (int i = 0; i < items.size(); i++) {
            SaleItem item = items.get(i);
            BigDecimal lineSubtotal = money(item.getSubtotal());
            boolean last = i == items.size() - 1;

            BigDecimal orderDiscountShare;
            if (last) {
                orderDiscountShare = orderDiscount.subtract(allocatedOrderDiscount);
            } else if (afterItemDiscounts.signum() > 0) {
                orderDiscountShare = orderDiscount.multiply(lineSubtotal)
                    .divide(afterItemDiscounts, 2, RoundingMode.HALF_UP);
            } else {
                orderDiscountShare = ZERO;
            }

            allocatedOrderDiscount = allocatedOrderDiscount.add(orderDiscountShare);

            BigDecimal lineBase = lineSubtotal.subtract(orderDiscountShare);
            if (lineBase.signum() < 0) {
                lineBase = ZERO;
            }

            result.put(item.getId(), money(lineBase));
        }

        return result;
    }

    private SaleReturnResponse toResponse(SaleReturn refund) {
        Sale sale = refund.getSale();

        List<SaleReturnResponse.Item> items = returnItemRepository
            .findAllBySaleReturn_IdOrderByIdAsc(refund.getId())
            .stream()
            .map(line -> new SaleReturnResponse.Item(
                line.getId(),
                line.getSaleItem().getInventoryItem().getId(),
                line.getSaleItem().getInventoryItem().getName(),
                line.getBatch().getId(),
                line.getBatch().getBatchNumber(),
                line.getQuantity(),
                line.getStockDisposition(),
                line.getRefundAmount()
            ))
            .toList();

        User user = refund.getCreatedBy();

        return new SaleReturnResponse(
            refund.getId(),
            refund.getRefundNumber(),
            sale.getId(),
            sale.getInvoiceNumber(),
            sale.getCustomerName(),
            refund.getReason(),
            refund.getRefundAmount(),
            refund.getStatus(),
            refund.getCreatedAt(),
            user == null ? null : user.getId(),
            user == null ? null : user.getFullName(),
            items
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    private record PreparedLine(
        SaleItem saleItem,
        TenantInventoryBatch batch,
        TenantInventoryItem inventoryItem,
        int quantity,
        BigDecimal refundAmount,
        BigDecimal commissionReversedAmount
    ) {}
}
