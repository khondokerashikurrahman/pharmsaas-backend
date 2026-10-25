package com.pharmsaas.backend.modules.inventory.repository;

import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface TenantInventoryRepository
        extends JpaRepository<TenantInventoryItem, Long> {

    List<TenantInventoryItem> findAllByTenant_IdOrderByNameAsc(Long tenantId);

    Optional<TenantInventoryItem> findByIdAndTenant_Id(Long id, Long tenantId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from TenantInventoryItem i where i.id = :itemId and i.tenant.id = :tenantId")
    Optional<TenantInventoryItem> findForUpdate(
        @Param("itemId") Long itemId,
        @Param("tenantId") Long tenantId
    );

    boolean existsByTenant_IdAndSkuIgnoreCase(Long tenantId, String sku);

    boolean existsByTenant_IdAndSkuIgnoreCaseAndIdNot(
        Long tenantId, String sku, Long id
    );
}
