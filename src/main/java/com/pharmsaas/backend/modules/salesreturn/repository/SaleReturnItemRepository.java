package com.pharmsaas.backend.modules.salesreturn.repository;

import com.pharmsaas.backend.modules.salesreturn.entity.SaleReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SaleReturnItemRepository extends JpaRepository<SaleReturnItem, Long> {
    List<SaleReturnItem> findAllBySaleItem_Id(Long saleItemId);
    List<SaleReturnItem> findAllBySaleReturn_IdOrderByIdAsc(Long saleReturnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ri from SaleReturnItem ri " +
           "where ri.id = :itemId " +
           "and ri.saleReturn.id = :returnId " +
           "and ri.saleReturn.tenant.id = :tenantId")
    Optional<SaleReturnItem> findForUpdateByIdAndReturnAndTenant(
        @Param("itemId") Long itemId,
        @Param("returnId") Long returnId,
        @Param("tenantId") Long tenantId
    );
}
