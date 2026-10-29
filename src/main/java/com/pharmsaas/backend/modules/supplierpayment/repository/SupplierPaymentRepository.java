package com.pharmsaas.backend.modules.supplierpayment.repository;

import com.pharmsaas.backend.modules.supplierpayment.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierPaymentRepository
        extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findAllByTenant_IdOrderByPaymentDateDescCreatedAtDesc(
        Long tenantId
    );

    List<SupplierPayment>
    findAllByTenant_IdAndSupplier_IdOrderByPaymentDateDescCreatedAtDesc(
        Long tenantId,
        Long supplierId
    );

    Optional<SupplierPayment> findByIdAndTenant_Id(
        Long id,
        Long tenantId
    );

    boolean existsByTenant_IdAndReferenceNumberIgnoreCase(
        Long tenantId,
        String referenceNumber
    );
}
