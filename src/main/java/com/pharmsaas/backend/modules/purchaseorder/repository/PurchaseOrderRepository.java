package com.pharmsaas.backend.modules.purchaseorder.repository;

import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    Optional<PurchaseOrder> findByIdAndTenant_Id(Long id, Long tenantId);
}
