package com.pharmsaas.backend.modules.inventory.repository;

import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TenantInventoryBatchRepository
        extends JpaRepository<TenantInventoryBatch, Long> {

    List<TenantInventoryBatch>
    findAllByTenant_IdAndInventoryItem_IdOrderByExpiryDateAsc(
        Long tenantId, Long inventoryItemId
    );

    List<TenantInventoryBatch>
    findAllByTenant_IdAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
        Long tenantId, LocalDate date
    );

    List<TenantInventoryBatch>
    findAllByTenant_IdAndExpiryDateBetweenOrderByExpiryDateAsc(
        Long tenantId, LocalDate start, LocalDate end
    );

    boolean existsByTenant_IdAndInventoryItem_IdAndBatchNumberIgnoreCase(
        Long tenantId, Long inventoryItemId, String batchNumber
    );

    Optional<TenantInventoryBatch>
    findByIdAndTenant_Id(Long batchId, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from TenantInventoryBatch b
        where b.id = :batchId
          and b.tenant.id = :tenantId
    """)
    Optional<TenantInventoryBatch> findForUpdate(
        @Param("batchId") Long batchId,
        @Param("tenantId") Long tenantId
    );
}
