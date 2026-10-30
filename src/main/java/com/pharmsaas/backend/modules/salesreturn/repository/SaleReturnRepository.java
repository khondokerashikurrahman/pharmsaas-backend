package com.pharmsaas.backend.modules.salesreturn.repository;

import com.pharmsaas.backend.modules.salesreturn.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {
    Optional<SaleReturn> findByTenant_IdAndRequestKey(Long tenantId, String requestKey);

    List<SaleReturn> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SaleReturn r where r.id = :returnId and r.tenant.id = :tenantId")
    Optional<SaleReturn> findForUpdateByIdAndTenant(
        @Param("returnId") Long returnId,
        @Param("tenantId") Long tenantId
    );
}
