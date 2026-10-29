package com.pharmsaas.backend.modules.supplierledger.service;

import com.pharmsaas.backend.modules.grn.entity.GoodsReceivedNote;
import com.pharmsaas.backend.modules.grn.repository.GoodsReceivedNoteRepository;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.supplier.entity.Supplier;
import com.pharmsaas.backend.modules.supplier.repository.SupplierRepository;
import com.pharmsaas.backend.modules.supplierpayment.entity.SupplierPayment;
import com.pharmsaas.backend.modules.supplierpayment.repository.SupplierPaymentRepository;
import com.pharmsaas.backend.modules.supplierledger.dto.SupplierLedgerResponse;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class SupplierLedgerService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private static final Set<MembershipRole> READ_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER,
        MembershipRole.STORE_KEEPER
    );

    private final SupplierRepository supplierRepository;
    private final SupplierPaymentRepository paymentRepository;
    private final GoodsReceivedNoteRepository grnRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public SupplierLedgerService(
            SupplierRepository supplierRepository,
            SupplierPaymentRepository paymentRepository,
            GoodsReceivedNoteRepository grnRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository) {
        this.supplierRepository = supplierRepository;
        this.paymentRepository = paymentRepository;
        this.grnRepository = grnRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    private void authorize(Long tenantId, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (tenant.getStatus() == null ||
                !"ACTIVE".equalsIgnoreCase(tenant.getStatus().trim())) {
            throw error(HttpStatus.FORBIDDEN, "Tenant must be Active");
        }

        var membership = membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> error(
                HttpStatus.FORBIDDEN, "Active tenant membership required"));

        if (membership.getRole() == null ||
                !READ_ROLES.contains(membership.getRole())) {
            throw error(
                HttpStatus.FORBIDDEN,
                "Your role cannot access the supplier ledger");
        }
    }

    @Transactional(readOnly = true)
    public SupplierLedgerResponse getLedger(
            Long tenantId, Long userId, Long supplierId) {

        authorize(tenantId, userId);

        Supplier supplier = supplierRepository
            .findByIdAndTenant_Id(supplierId, tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Supplier not found for this tenant"));

        BigDecimal opening = money(supplier.getOpeningBalance());
        BigDecimal totalPurchases = ZERO;
        BigDecimal totalPayments = ZERO;

        List<Entry> entries = new ArrayList<>();

        if (opening.compareTo(BigDecimal.ZERO) != 0) {
            entries.add(new Entry(
                null, 0, "OPENING", "Adjustment", "OPENING-BALANCE",
                "Opening balance", opening, ZERO, "OPENING_BALANCE"
            ));
        }

        /*
         * Only received GRNs count as purchases.
         * A draft or submitted PO alone does not increase supplier debt.
         * Legacy GRNs without supplier_id use the stored supplier name.
         */
        for (GoodsReceivedNote grn :
                grnRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)) {

            if (!"RECEIVED".equalsIgnoreCase(grn.getStatus())) {
                continue;
            }

            boolean sameSupplier;
            if (grn.getSupplier() != null) {
                sameSupplier = supplierId.equals(grn.getSupplier().getId());
            } else {
                sameSupplier = grn.getSupplierName() != null
                    && supplier.getName() != null
                    && grn.getSupplierName().trim()
                        .equalsIgnoreCase(supplier.getName().trim());
            }

            if (!sameSupplier) {
                continue;
            }

            BigDecimal grnTotal = ZERO;
            for (var item : grn.getItems()) {
                if (item.getPurchasePrice() != null &&
                        item.getQuantity() != null &&
                        item.getQuantity() > 0) {
                    grnTotal = grnTotal.add(
                        item.getPurchasePrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                }
            }

            grnTotal = money(grnTotal);
            totalPurchases = totalPurchases.add(grnTotal);

            entries.add(new Entry(
                grn.getReceivedDate(), 1,
                "GRN-" + grn.getId(), "Purchase", grn.getGrnNumber(),
                "Goods received from supplier", grnTotal, ZERO, "RECEIVED"
            ));
        }

        /*
         * Paid payments reduce payable debt.
         * Cancelled payments remain visible in history but do not reduce debt.
         */
        List<SupplierPayment> payments =
            paymentRepository
                .findAllByTenant_IdAndSupplier_IdOrderByPaymentDateDescCreatedAtDesc(
                    tenantId, supplierId);

        for (SupplierPayment payment : payments) {
            boolean paid = "PAID".equalsIgnoreCase(payment.getStatus());
            BigDecimal amount = money(payment.getAmount());
            BigDecimal credit = paid ? amount : ZERO;

            if (paid) {
                totalPayments = totalPayments.add(amount);
            }

            String description = paid
                ? "Supplier payment recorded"
                : "Cancelled payment; excluded from balance";

            entries.add(new Entry(
                payment.getPaymentDate(), 2,
                "PAY-" + payment.getId(), "Payment",
                payment.getPaymentNumber(), description,
                ZERO, credit, payment.getStatus()
            ));
        }

        entries.sort(
            Comparator.comparing(
                Entry::date,
                Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingInt(Entry::order)
                .thenComparing(Entry::reference)
        );

        totalPurchases = money(totalPurchases);
        totalPayments = money(totalPayments);

        BigDecimal balance = opening;
        List<SupplierLedgerResponse.Transaction> transactions =
            new ArrayList<>();

        for (Entry entry : entries) {
            balance = balance.add(entry.debit()).subtract(entry.credit());

            transactions.add(new SupplierLedgerResponse.Transaction(
                entry.id(),
                entry.date(),
                entry.type(),
                entry.reference(),
                entry.description(),
                money(entry.debit()),
                money(entry.credit()),
                money(balance),
                entry.status()
            ));
        }

        BigDecimal outstanding = money(
            opening.add(totalPurchases).subtract(totalPayments));

        return new SupplierLedgerResponse(
            supplier.getId(),
            supplier.getSupplierCode(),
            supplier.getName(),
            opening,
            totalPurchases,
            totalPayments,
            outstanding,
            transactions
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
            ? ZERO
            : value.setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException error(
            HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    private record Entry(
        LocalDate date,
        int order,
        String id,
        String type,
        String reference,
        String description,
        BigDecimal debit,
        BigDecimal credit,
        String status
    ) {}
}
