package com.pharmsaas.backend.modules.inventory.repository;

import com.pharmsaas.backend.modules.inventory.entity.StockRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {

    List<StockRequest> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    List<StockRequest> findAllByTenant_IdAndRequesterUserIdOrderByCreatedAtDesc(
        Long tenantId, Long requesterUserId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockRequest r where r.id = :id and r.tenant.id = :tenantId")
    Optional<StockRequest> findForUpdate(
        @Param("id") Long id, @Param("tenantId") Long tenantId
    );
}
