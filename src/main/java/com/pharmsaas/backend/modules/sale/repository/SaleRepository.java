package com.pharmsaas.backend.modules.sale.repository;

import com.pharmsaas.backend.modules.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);
    Optional<Sale> findByIdAndTenant_Id(Long id, Long tenantId);
    boolean existsByTenant_IdAndInvoiceNumber(Long tenantId, String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :saleId and s.tenant.id = :tenantId")
    Optional<Sale> findForUpdateByIdAndTenant_Id(
        @Param("saleId") Long saleId,
        @Param("tenantId") Long tenantId
    );
}
