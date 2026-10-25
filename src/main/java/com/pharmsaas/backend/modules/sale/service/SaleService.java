package com.pharmsaas.backend.modules.sale.service;

import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryRepository;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.sale.dto.SaleCreateRequest;
import com.pharmsaas.backend.modules.sale.dto.SaleResponse;
import com.pharmsaas.backend.modules.sale.entity.Sale;
import com.pharmsaas.backend.modules.sale.entity.SaleItem;
import com.pharmsaas.backend.modules.sale.repository.SaleItemRepository;
import com.pharmsaas.backend.modules.sale.repository.SaleRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SaleService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final TenantInventoryRepository inventoryRepository;
    private final TenantInventoryBatchRepository batchRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public SaleService(
        SaleRepository saleRepository,
        SaleItemRepository saleItemRepository,
        TenantInventoryRepository inventoryRepository,
        TenantInventoryBatchRepository batchRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository
    ) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public SaleResponse create(Long tenantId, Long userId, SaleCreateRequest request) {
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

        String role = membership.getRole().name();
        if (!Set.of(
            "TENANT_OWNER", "PHARMACY_ADMIN", "MANAGER",
            "STORE_KEEPER", "PHARMACIST", "STAFF", "CASHIER"
        ).contains(role)) {
            throw error(HttpStatus.FORBIDDEN, "You cannot create sales");
        }

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "Sale must contain at least one item");
        }

        String paymentMethod = request.paymentMethod() == null
            ? "" : request.paymentMethod().trim().toUpperCase(Locale.ROOT);

        if (!Set.of("CASH", "CARD", "MOBILE").contains(paymentMethod)) {
            throw error(HttpStatus.BAD_REQUEST, "Payment method must be Cash, Card or Mobile");
        }

        String discountType = request.discountType() == null
            ? "amount" : request.discountType().trim().toLowerCase(Locale.ROOT);

        if (!Set.of("amount", "percentage").contains(discountType)) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid discount type");
        }

        BigDecimal discountValue = money(request.discountValue());
        BigDecimal taxRate = request.taxRate() == null ? ZERO : request.taxRate();

        if (discountValue.signum() < 0 ||
            taxRate.signum() < 0 ||
            taxRate.compareTo(HUNDRED) > 0) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid discount or tax value");
        }

        if ("percentage".equals(discountType) &&
            discountValue.compareTo(HUNDRED) > 0) {
            throw error(HttpStatus.BAD_REQUEST, "Discount percentage cannot exceed 100");
        }

        Set<Long> usedBatches = new HashSet<>();
        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal subtotal = ZERO;

        for (SaleCreateRequest.SaleLineRequest line : request.items()) {
            if (line == null || line.productId() == null ||
                line.batchId() == null || line.quantity() == null ||
                line.quantity() <= 0) {
                throw error(HttpStatus.BAD_REQUEST, "Each item needs a product, batch and positive quantity");
            }

            if (!usedBatches.add(line.batchId())) {
                throw error(HttpStatus.BAD_REQUEST, "Duplicate batch in sale; combine its quantity");
            }

            TenantInventoryItem item = inventoryRepository
                .findForUpdate(line.productId(), tenantId)
                .orElseThrow(() -> error(
                    HttpStatus.NOT_FOUND, "Inventory product not found"
                ));

            TenantInventoryBatch batch = batchRepository
                .findForUpdate(line.batchId(), tenantId)
                .orElseThrow(() -> error(
                    HttpStatus.NOT_FOUND, "Inventory batch not found"
                ));

            if (batch.getInventoryItem() == null ||
                !batch.getInventoryItem().getId().equals(item.getId())) {
                throw error(HttpStatus.BAD_REQUEST, "Batch does not belong to this product");
            }

            if (batch.getExpiryDate() != null &&
                !batch.getExpiryDate().isAfter(LocalDate.now())) {
                throw error(HttpStatus.BAD_REQUEST, "Expired medicine cannot be sold");
            }

            if (batch.getQuantity() == null ||
                batch.getQuantity() < line.quantity()) {
                throw error(HttpStatus.CONFLICT, "Insufficient stock in batch");
            }

            if (item.getStockQuantity() == null ||
                item.getStockQuantity() < line.quantity()) {
                throw error(HttpStatus.CONFLICT, "Insufficient inventory stock");
            }

            BigDecimal unitPrice = money(item.getSellingPrice());
            if (unitPrice.signum() < 0) {
                throw error(HttpStatus.BAD_REQUEST, "Invalid product selling price");
            }

            BigDecimal lineTotal = unitPrice
                .multiply(BigDecimal.valueOf(line.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

            SaleItem saleItem = new SaleItem();
            saleItem.setInventoryItem(item);
            saleItem.setBatch(batch);
            saleItem.setQuantity(line.quantity());
            saleItem.setUnitPrice(unitPrice);
            saleItem.setSubtotal(lineTotal);
            saleItems.add(saleItem);

            subtotal = subtotal.add(lineTotal);

            batch.setQuantity(batch.getQuantity() - line.quantity());
            item.setStockQuantity(item.getStockQuantity() - line.quantity());

            batchRepository.save(batch);
            inventoryRepository.save(item);
        }

        BigDecimal discount = "percentage".equals(discountType)
            ? subtotal.multiply(discountValue)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP)
            : discountValue.setScale(2, RoundingMode.HALF_UP);

        if (discount.compareTo(subtotal) > 0) {
            throw error(HttpStatus.BAD_REQUEST, "Discount cannot exceed subtotal");
        }

        BigDecimal taxableAmount = subtotal.subtract(discount);
        BigDecimal tax = taxableAmount.multiply(taxRate)
            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = taxableAmount.add(tax)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal amountReceived;
        BigDecimal changeAmount;

        if ("CASH".equals(paymentMethod)) {
            amountReceived = money(request.amountReceived());
            if (amountReceived.compareTo(total) < 0) {
                throw error(HttpStatus.BAD_REQUEST, "Cash received is less than total");
            }
            changeAmount = amountReceived.subtract(total)
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            amountReceived = total;
            changeAmount = ZERO;
        }

        Sale sale = new Sale();
        sale.setTenant(tenant);
        sale.setInvoiceNumber("INV-" + UUID.randomUUID()
            .toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        sale.setCustomerName(
            request.customerName() == null || request.customerName().isBlank()
                ? "Walk-in Customer" : request.customerName().trim()
        );
        sale.setPaymentMethod(paymentMethod);
        sale.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        sale.setDiscount(discount);
        sale.setDiscountType(discountType);
        sale.setDiscountValue(discountValue);
        sale.setTax(tax);
        sale.setTotal(total);
        sale.setAmountReceived(amountReceived);
        sale.setChangeAmount(changeAmount);
        sale.setStatus("COMPLETED");

        saleRepository.saveAndFlush(sale);

        for (SaleItem saleItem : saleItems) {
            saleItem.setSale(sale);
        }
        saleItemRepository.saveAll(saleItems);

        return toResponse(sale, saleItems);
    }

    @Transactional
    public List<SaleResponse> list(Long tenantId, Long userId) {
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

        if (!Set.of(
            "TENANT_OWNER", "PHARMACY_ADMIN", "MANAGER",
            "STORE_KEEPER", "PHARMACIST", "STAFF", "CASHIER"
        ).contains(membership.getRole().name())) {
            throw error(HttpStatus.FORBIDDEN, "You cannot view sales");
        }

        return saleRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
            .stream()
            .map(sale -> toResponse(
                sale,
                saleItemRepository.findAllBySale_IdOrderByIdAsc(sale.getId())
            ))
            .toList();
    }

    private SaleResponse toResponse(Sale sale, List<SaleItem> items) {
        List<SaleResponse.Item> lines = items.stream().map(line ->
            new SaleResponse.Item(
                line.getInventoryItem().getId(),
                line.getInventoryItem().getName(),
                line.getInventoryItem().getSku(),
                line.getBatch().getId(),
                line.getBatch().getBatchNumber(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getSubtotal()
            )
        ).toList();

        return new SaleResponse(
            sale.getId(),
            sale.getInvoiceNumber(),
            sale.getCustomerName(),
            sale.getPaymentMethod(),
            sale.getSubtotal(),
            sale.getDiscount(),
            sale.getDiscountType(),
            sale.getDiscountValue(),
            sale.getTax(),
            sale.getTotal(),
            sale.getAmountReceived(),
            sale.getChangeAmount(),
            sale.getStatus(),
            sale.getCreatedAt(),
            lines
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
