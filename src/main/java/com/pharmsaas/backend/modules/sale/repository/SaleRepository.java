package com.pharmsaas.backend.modules.sale.repository;

import com.pharmsaas.backend.modules.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);
    Optional<Sale> findByIdAndTenant_Id(Long id, Long tenantId);
    boolean existsByTenant_IdAndInvoiceNumber(Long tenantId, String invoiceNumber);
}
