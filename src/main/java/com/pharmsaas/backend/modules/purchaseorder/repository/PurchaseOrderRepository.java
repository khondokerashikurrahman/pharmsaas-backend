package com.pharmsaas.backend.modules.purchaseorder.repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    Optional<PurchaseOrder> findByIdAndTenant_Id(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseOrder> findByTenant_IdAndOrderNumberIgnoreCase(
        Long tenantId, String orderNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p from PurchaseOrder p
        where p.tenant.id = :tenantId
          and lower(p.orderNumber) = lower(:orderNumber)
    """)
    Optional<PurchaseOrder> findForUpdateByTenantAndOrderNumber(
        @Param("tenantId") Long tenantId,
        @Param("orderNumber") String orderNumber
    );

}
