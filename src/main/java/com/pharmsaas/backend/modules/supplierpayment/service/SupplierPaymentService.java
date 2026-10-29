package com.pharmsaas.backend.modules.supplierpayment.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.supplier.repository.SupplierRepository;
import com.pharmsaas.backend.modules.supplierpayment.dto.SupplierPaymentRequest;
import com.pharmsaas.backend.modules.supplierpayment.dto.SupplierPaymentResponse;
import com.pharmsaas.backend.modules.supplierpayment.entity.SupplierPayment;
import com.pharmsaas.backend.modules.supplierpayment.repository.SupplierPaymentRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SupplierPaymentService {

    private static final Set<MembershipRole> WRITE_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER
    );

    private static final Set<MembershipRole> READ_ROLES = Set.of(
        MembershipRole.TENANT_OWNER,
        MembershipRole.PHARMACY_ADMIN,
        MembershipRole.MANAGER,
        MembershipRole.STORE_KEEPER
    );

    private final SupplierPaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public SupplierPaymentService(
            SupplierPaymentRepository paymentRepository,
            SupplierRepository supplierRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository) {
        this.paymentRepository = paymentRepository;
        this.supplierRepository = supplierRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    private Tenant authorize(Long tenantId, Long userId, boolean write) {
        if (tenantId == null || tenantId <= 0 ||
                userId == null || userId <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Valid tenant and user IDs are required");
        }

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

        Set<MembershipRole> allowed = write ? WRITE_ROLES : READ_ROLES;
        if (membership.getRole() == null ||
                !allowed.contains(membership.getRole())) {
            throw error(HttpStatus.FORBIDDEN,
                write
                    ? "Your role cannot record supplier payments"
                    : "Your role cannot view supplier payments");
        }

        return tenant;
    }

    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> list(
            Long tenantId, Long userId, Long supplierId) {
        authorize(tenantId, userId, false);

        List<SupplierPayment> rows = supplierId == null
            ? paymentRepository.findAllByTenant_IdOrderByPaymentDateDescCreatedAtDesc(tenantId)
            : paymentRepository.findAllByTenant_IdAndSupplier_IdOrderByPaymentDateDescCreatedAtDesc(
                tenantId, supplierId);

        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SupplierPaymentResponse get(
            Long tenantId, Long userId, Long paymentId) {
        authorize(tenantId, userId, false);
        return toResponse(findPayment(tenantId, paymentId));
    }

    @Transactional
    public SupplierPaymentResponse create(
            Long tenantId, Long userId, SupplierPaymentRequest request) {
        Tenant tenant = authorize(tenantId, userId, true);
        validate(request);

        var supplier = supplierRepository
            .findByIdAndTenant_Id(request.supplierId(), tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Supplier not found for this tenant"));

        String reference = clean(request.referenceNumber());
        if (reference != null &&
                paymentRepository.existsByTenant_IdAndReferenceNumberIgnoreCase(
                    tenantId, reference)) {
            throw error(
                HttpStatus.CONFLICT,
                "This payment reference has already been recorded for this tenant");
        }

        SupplierPayment payment = new SupplierPayment();
        payment.setTenant(tenant);
        payment.setSupplier(supplier);
        payment.setPaymentNumber(
            "PAY-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase(Locale.ROOT));
        payment.setPaymentDate(request.paymentDate());
        payment.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        payment.setPaymentMethod(normalizeMethod(request.paymentMethod()));
        payment.setReferenceNumber(reference);
        payment.setNotes(clean(request.notes()));
        payment.setStatus("PAID");
        payment.setCreatedBy(userId);

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public SupplierPaymentResponse cancel(
            Long tenantId, Long userId, Long paymentId) {
        authorize(tenantId, userId, true);
        SupplierPayment payment = findPayment(tenantId, paymentId);

        if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
            throw error(
                HttpStatus.CONFLICT,
                "Only a recorded Paid payment can be cancelled");
        }

        payment.setStatus("CANCELLED");
        return toResponse(paymentRepository.save(payment));
    }

    private SupplierPayment findPayment(Long tenantId, Long paymentId) {
        return paymentRepository.findByIdAndTenant_Id(paymentId, tenantId)
            .orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "Supplier payment not found"));
    }

    private void validate(SupplierPaymentRequest r) {
        if (r == null) {
            throw error(HttpStatus.BAD_REQUEST, "Payment request is required");
        }
        if (r.supplierId() == null || r.supplierId() <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Select a valid supplier");
        }
        if (r.paymentDate() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Payment date is required");
        }
        if (r.paymentDate().isAfter(LocalDate.now())) {
            throw error(HttpStatus.BAD_REQUEST, "Payment date cannot be in the future");
        }
        if (r.amount() == null || r.amount().signum() <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }
        if (r.amount().stripTrailingZeros().scale() > 2) {
            throw error(HttpStatus.BAD_REQUEST, "Payment amount cannot have more than two decimal places");
        }
        if (r.amount().compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw error(HttpStatus.BAD_REQUEST, "Payment amount is too large");
        }
        if (r.paymentMethod() == null ||
                !Set.of("CASH", "BANK", "BANK_TRANSFER", "CARD", "MOBILE_BANKING")
                    .contains(r.paymentMethod().trim()
                        .toUpperCase(Locale.ROOT).replace(' ', '_'))) {
            throw error(
                HttpStatus.BAD_REQUEST,
                "Payment method must be Cash, Bank Transfer, Card or Mobile Banking");
        }
        if (r.referenceNumber() != null &&
                r.referenceNumber().trim().length() > 100) {
            throw error(HttpStatus.BAD_REQUEST, "Reference cannot exceed 100 characters");
        }
        if (r.notes() != null && r.notes().trim().length() > 1000) {
            throw error(HttpStatus.BAD_REQUEST, "Notes cannot exceed 1000 characters");
        }
    }

    private String normalizeMethod(String method) {
        return switch (method.trim().toUpperCase(Locale.ROOT).replace(' ', '_')) {
            case "CASH" -> "Cash";
            case "BANK", "BANK_TRANSFER" -> "Bank Transfer";
            case "CARD" -> "Card";
            case "MOBILE_BANKING" -> "Mobile Banking";
            default -> throw error(HttpStatus.BAD_REQUEST, "Unsupported payment method");
        };
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SupplierPaymentResponse toResponse(SupplierPayment p) {
        return new SupplierPaymentResponse(
            p.getId(),
            p.getTenant().getId(),
            p.getSupplier().getId(),
            p.getSupplier().getSupplierCode(),
            p.getSupplier().getName(),
            p.getPaymentNumber(),
            p.getPaymentDate(),
            p.getAmount(),
            p.getPaymentMethod(),
            p.getReferenceNumber(),
            p.getNotes(),
            p.getStatus(),
            p.getCreatedBy(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
