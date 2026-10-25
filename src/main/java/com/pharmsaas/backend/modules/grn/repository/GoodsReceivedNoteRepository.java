package com.pharmsaas.backend.modules.grn.repository;

import com.pharmsaas.backend.modules.grn.entity.GoodsReceivedNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GoodsReceivedNoteRepository extends JpaRepository<GoodsReceivedNote, Long> {
    List<GoodsReceivedNote> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);
    Optional<GoodsReceivedNote> findByIdAndTenant_Id(Long id, Long tenantId);
    boolean existsByTenant_IdAndSupplierInvoiceNumberIgnoreCase(Long tenantId, String invoiceNumber);
}
