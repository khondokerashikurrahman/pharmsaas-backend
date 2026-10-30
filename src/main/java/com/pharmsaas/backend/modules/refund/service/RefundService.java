package com.pharmsaas.backend.modules.refund.service;

import com.pharmsaas.backend.modules.refund.dto.*;
import com.pharmsaas.backend.modules.refund.entity.Refund;
import com.pharmsaas.backend.modules.refund.repository.RefundRepository;
import com.pharmsaas.backend.modules.salesreturn.entity.SaleReturn;
import com.pharmsaas.backend.modules.salesreturn.repository.SaleReturnRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.user.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class RefundService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final Set<String> METHODS =
        Set.of("CASH", "CARD", "MOBILE_BANKING", "BANK_TRANSFER");
    private static final Set<String> MANAGERS =
        Set.of("TENANT_OWNER", "PHARMACY_ADMIN", "MANAGER");

    private final RefundRepository refundRepository;
    private final SaleReturnRepository returnRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public RefundService(
        RefundRepository refundRepository,
        SaleReturnRepository returnRepository,
        TenantRepository tenantRepository,
        TenantMembershipRepository membershipRepository
    ) {
        this.refundRepository = refundRepository;
        this.returnRepository = returnRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public RefundResponse create(Long tenantId, Long returnId, Long userId,
                                 RefundCreateRequest request) {
        validateTenant(tenantId);
        var membership = requireManager(tenantId, userId);

        if (request == null || request.requestKey() == null
            || request.requestKey().isBlank() || request.requestKey().length() > 100) {
            throw error(HttpStatus.BAD_REQUEST, "A valid refund request key is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "Refund amount must be greater than zero");
        }
        if (request.paymentMethod() == null
            || !METHODS.contains(request.paymentMethod().trim().toUpperCase(Locale.ROOT))) {
            throw error(HttpStatus.BAD_REQUEST, "Unsupported refund payment method");
        }
        if (request.notes() != null && request.notes().length() > 1000) {
            throw error(HttpStatus.BAD_REQUEST, "Refund notes cannot exceed 1000 characters");
        }
        if (request.transactionReference() != null
            && request.transactionReference().length() > 150) {
            throw error(HttpStatus.BAD_REQUEST, "Transaction reference is too long");
        }

        SaleReturn saleReturn = returnRepository.findForUpdateByIdAndTenant(returnId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Sale return not found"));

        String key = request.requestKey().trim();
        var existing = refundRepository.findByTenant_IdAndRequestKey(tenantId, key);
        if (existing.isPresent()) {
            Refund previous = existing.get();
            if (!previous.getSaleReturn().getId().equals(returnId)
                || previous.getAmount().compareTo(request.amount().setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw error(HttpStatus.CONFLICT, "This request key was already used");
            }
            return toResponse(previous);
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal reserved = refundRepository.sumReservedAmount(tenantId, returnId);
        if (reserved == null) reserved = ZERO;

        BigDecimal eligible = saleReturn.getRefundAmount() == null
            ? ZERO : saleReturn.getRefundAmount().setScale(2, RoundingMode.HALF_UP);

        if (reserved.add(amount).compareTo(eligible) > 0) {
            throw error(HttpStatus.CONFLICT,
                "Refund exceeds the remaining refundable amount. Remaining: "
                + eligible.subtract(reserved).max(ZERO));
        }

        Refund refund = new Refund();
        refund.setTenant(saleReturn.getTenant());
        refund.setSaleReturn(saleReturn);
        refund.setCreatedBy(membership.getUser());
        refund.setRefundNumber("RFD-" +
            UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        refund.setRequestKey(key);
        refund.setAmount(amount);
        refund.setPaymentMethod(request.paymentMethod().trim().toUpperCase(Locale.ROOT));
        refund.setTransactionReference(clean(request.transactionReference()));
        refund.setNotes(clean(request.notes()));
        refund.setStatus("PENDING");

        return toResponse(refundRepository.saveAndFlush(refund));
    }

    @Transactional
    public List<RefundResponse> list(Long tenantId, Long userId) {
        validateTenant(tenantId);
        requireManager(tenantId, userId);
        return refundRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public RefundResponse complete(Long tenantId, Long refundId, Long userId,
                                   RefundProcessRequest request) {
        return process(tenantId, refundId, userId, request, "COMPLETED");
    }

    @Transactional
    public RefundResponse fail(Long tenantId, Long refundId, Long userId,
                               RefundProcessRequest request) {
        return process(tenantId, refundId, userId, request, "FAILED");
    }

    private RefundResponse process(Long tenantId, Long refundId, Long userId,
                                    RefundProcessRequest request, String nextStatus) {
        validateTenant(tenantId);
        var membership = requireManager(tenantId, userId);

        Refund refund = refundRepository.findForUpdate(refundId, tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Refund not found"));

        if (!"PENDING".equals(refund.getStatus())) {
            throw error(HttpStatus.CONFLICT, "Only pending refunds can be processed");
        }

        String reference = request == null ? null : clean(request.transactionReference());
        String notes = request == null ? null : clean(request.notes());

        if ("COMPLETED".equals(nextStatus)
            && !"CASH".equals(refund.getPaymentMethod())
            && (reference == null || reference.isBlank())) {
            throw error(HttpStatus.BAD_REQUEST,
                "Transaction reference is required to complete a non-cash refund");
        }

        if (reference != null) refund.setTransactionReference(reference);
        if (notes != null) refund.setNotes(notes);
        refund.setStatus(nextStatus);
        refund.setProcessedBy(membership.getUser());
        refund.setProcessedAt(OffsetDateTime.now());

        return toResponse(refundRepository.save(refund));
    }

    private void validateTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Tenant not found"));
        if (tenant.getStatus() == null || !tenant.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw error(HttpStatus.FORBIDDEN, "Tenant is not active");
        }
    }

    private com.pharmsaas.backend.modules.membership.entity.TenantMembership requireManager(Long tenantId, Long userId) {
        var membership = membershipRepository.findByTenant_IdAndUser_IdAndActiveTrue(tenantId, userId)
            .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Active tenant membership required"));
        if (!MANAGERS.contains(membership.getRole().name())) {
            throw error(HttpStatus.FORBIDDEN, "Only tenant owner, admin or manager can process refunds");
        }
        return membership;
    }

    private RefundResponse toResponse(Refund refund) {
        User creator = refund.getCreatedBy();
        User processor = refund.getProcessedBy();
        return new RefundResponse(
            refund.getId(),
            refund.getSaleReturn().getId(),
            refund.getRefundNumber(),
            refund.getRequestKey(),
            refund.getAmount(),
            refund.getPaymentMethod(),
            refund.getTransactionReference(),
            refund.getStatus(),
            refund.getNotes(),
            refund.getCreatedAt(),
            refund.getProcessedAt(),
            creator == null ? null : creator.getId(),
            processor == null ? null : processor.getId()
        );
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

}
