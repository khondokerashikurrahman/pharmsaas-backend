package com.pharmsaas.backend.modules.refund.repository;

import com.pharmsaas.backend.modules.refund.entity.Refund;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByTenant_IdAndRequestKey(Long tenantId, String requestKey);

    List<Refund> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r " +
           "where r.tenant.id = :tenantId and r.saleReturn.id = :returnId " +
           "and r.status in ('PENDING', 'COMPLETED')")
    BigDecimal sumReservedAmount(
        @Param("tenantId") Long tenantId,
        @Param("returnId") Long returnId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :id and r.tenant.id = :tenantId")
    Optional<Refund> findForUpdate(
        @Param("id") Long id,
        @Param("tenantId") Long tenantId
    );
}
