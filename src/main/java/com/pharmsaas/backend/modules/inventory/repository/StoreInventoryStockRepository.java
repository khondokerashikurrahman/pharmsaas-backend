package com.pharmsaas.backend.modules.inventory.repository;

import com.pharmsaas.backend.modules.inventory.entity.StoreInventoryStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreInventoryStockRepository
        extends JpaRepository<StoreInventoryStock, Long> {

    List<StoreInventoryStock> findAllByTenant_IdAndStore_Id(
        Long tenantId,
        Long storeId
    );

    @Query("""
        select coalesce(sum(s.quantity), 0)
        from StoreInventoryStock s
        where s.tenant.id = :tenantId
          and s.batch.id = :batchId
    """)
    Long sumAllocatedQuantity(
        @Param("tenantId") Long tenantId,
        @Param("batchId") Long batchId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s from StoreInventoryStock s
        where s.tenant.id = :tenantId
          and s.store.id = :storeId
          and s.batch.id = :batchId
    """)
    Optional<StoreInventoryStock> findForUpdate(
        @Param("tenantId") Long tenantId,
        @Param("storeId") Long storeId,
        @Param("batchId") Long batchId
    );
}
