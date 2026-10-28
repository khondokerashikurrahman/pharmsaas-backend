package com.pharmsaas.backend.modules.purchaseorder.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.purchaseorder.dto.PurchaseOrderRequest;
import com.pharmsaas.backend.modules.purchaseorder.dto.PurchaseOrderResponse;
import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrder;
import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrderItem;
import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrderStatus;
import com.pharmsaas.backend.modules.purchaseorder.repository.PurchaseOrderRepository;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository orderRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository) {
        this.orderRepository = orderRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    private void authorize(Long tenantId, Long userId, boolean write) {
        var tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null ||
                !"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Tenant must be Active");
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
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot access Purchase Orders");
        }
    }

    private PurchaseOrder findOrder(Long tenantId, Long orderId) {
        return orderRepository.findByIdAndTenant_Id(orderId, tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Purchase Order not found"));
    }

    private void validateRequest(PurchaseOrderRequest request) {
        if (request == null) badRequest("Request body is required");
        if (request.supplierName() == null ||
                request.supplierName().isBlank()) {
            badRequest("Supplier name is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            badRequest("At least one item is required");
        }
        if (request.orderDate() == null) {
            badRequest("Order date is required");
        }
        if (request.expectedDeliveryDate() != null
                && request.expectedDeliveryDate().isBefore(request.orderDate())) {
            badRequest("Expected delivery date cannot be before order date");
        }
        nonNegative(request.discount(), "Order discount");
        nonNegative(request.tax(), "Order tax");

        for (var item : request.items()) {
            if (item == null || item.medicineName() == null
                    || item.medicineName().isBlank()) {
                badRequest("Each item must have a medicine name");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                badRequest("Item quantity must be greater than zero");
            }
            if (item.unitPrice() == null || item.unitPrice().signum() < 0) {
                badRequest("Item unit price cannot be negative");
            }
            nonNegative(item.discount(), "Item discount");
            nonNegative(item.tax(), "Item tax");
        }
    }

    private void nonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            badRequest(field + " cannot be negative");
        }
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private void fillOrder(PurchaseOrder order, PurchaseOrderRequest request) {
        validateRequest(request);
        order.setSupplierName(request.supplierName().trim());
        order.setBranchId(request.branchId());
        order.setOrderDate(request.orderDate());
        order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        order.setNotes(request.notes());
        order.setDiscount(zeroIfNull(request.discount()));
        order.setTax(zeroIfNull(request.tax()));

        List<PurchaseOrderItem> items = request.items().stream().map(input -> {
            BigDecimal price = input.unitPrice();
            BigDecimal itemDiscount = zeroIfNull(input.discount());
            BigDecimal itemTax = zeroIfNull(input.tax());

            BigDecimal lineSubtotal = price.multiply(
                BigDecimal.valueOf(input.quantity()));
            BigDecimal lineTotal = lineSubtotal
                .subtract(itemDiscount)
                .add(itemTax);

            if (lineTotal.signum() < 0) {
                badRequest("Item discount cannot exceed its subtotal plus tax");
            }

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setInventoryItemId(input.inventoryItemId());
            item.setMedicineName(input.medicineName().trim());
            item.setQuantity(input.quantity());
            item.setUnitPrice(price);
            item.setDiscount(itemDiscount);
            item.setTax(itemTax);
            item.setTotal(lineTotal);
            return item;
        }).toList();

        order.replaceItems(items);

        BigDecimal subtotal = items.stream()
            .map(i -> i.getUnitPrice().multiply(
                BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemDiscounts = items.stream()
            .map(PurchaseOrderItem::getDiscount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemTaxes = items.stream()
            .map(PurchaseOrderItem::getTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = subtotal.subtract(itemDiscounts)
            .add(itemTaxes)
            .subtract(order.getDiscount())
            .add(order.getTax());

        if (total.signum() < 0) {
            badRequest("Order total cannot be negative");
        }

        order.setSubtotal(subtotal);
        order.setTotalAmount(total);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private PurchaseOrderResponse response(PurchaseOrder order) {
        var items = order.getItems().stream().map(item ->
            new PurchaseOrderResponse.Item(
                item.getId(),
                item.getInventoryItemId(),
                item.getMedicineName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscount(),
                item.getTax(),
                item.getTotal()
            )
        ).toList();

        return new PurchaseOrderResponse(
            order.getId(),
            order.getTenant().getId(),
            order.getOrderNumber(),
            order.getSupplierName(),
            order.getBranchId(),
            order.getOrderDate(),
            order.getExpectedDeliveryDate(),
            order.getSubtotal(),
            order.getDiscount(),
            order.getTax(),
            order.getTotalAmount(),
            order.getStatus().name(),
            order.getNotes(),
            order.getCreatedBy(),
            order.getApprovedBy(),
            order.getApprovedAt(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            items
        );
    }

    @Transactional
    public PurchaseOrderResponse create(
            Long tenantId, Long userId, PurchaseOrderRequest request) {
        authorize(tenantId, userId, true);

        var tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Tenant not found"));

        PurchaseOrder order = new PurchaseOrder();
        order.setTenant(tenant);
        order.setOrderNumber("PO-" + UUID.randomUUID()
            .toString().substring(0, 8).toUpperCase());
        order.setCreatedBy(userId);
        order.setStatus(PurchaseOrderStatus.DRAFT);
        fillOrder(order, request);

        return response(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> list(Long tenantId, Long userId) {
        authorize(tenantId, userId, false);
        return orderRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
            .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(
            Long tenantId, Long userId, Long orderId) {
        authorize(tenantId, userId, false);
        return response(findOrder(tenantId, orderId));
    }

    @Transactional
    public PurchaseOrderResponse update(
            Long tenantId, Long userId, Long orderId,
            PurchaseOrderRequest request) {
        authorize(tenantId, userId, true);
        PurchaseOrder order = findOrder(tenantId, orderId);
        requireDraft(order);
        fillOrder(order, request);
        return response(orderRepository.save(order));
    }

    @Transactional
    public PurchaseOrderResponse submit(
            Long tenantId, Long userId, Long orderId) {
        authorize(tenantId, userId, true);
        PurchaseOrder order = findOrder(tenantId, orderId);
        requireDraft(order);
        order.setStatus(PurchaseOrderStatus.PENDING);
        return response(orderRepository.save(order));
    }

    @Transactional
    public PurchaseOrderResponse cancel(
            Long tenantId, Long userId, Long orderId) {
        authorize(tenantId, userId, true);
        PurchaseOrder order = findOrder(tenantId, orderId);
        if (order.getStatus() == PurchaseOrderStatus.RECEIVED
                || order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            badRequest("This Purchase Order cannot be cancelled");
        }
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return response(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long tenantId, Long userId, Long orderId) {
        authorize(tenantId, userId, true);
        PurchaseOrder order = findOrder(tenantId, orderId);
        requireDraft(order);
        orderRepository.delete(order);
    }

    private void requireDraft(PurchaseOrder order) {
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Only Draft Purchase Orders can be modified");
        }
    }
}
