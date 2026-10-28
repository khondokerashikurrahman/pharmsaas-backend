package com.pharmsaas.backend.modules.branchtransfer.repository;

import com.pharmsaas.backend.modules.branchtransfer.entity.BranchTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchTransferRepository
        extends JpaRepository<BranchTransfer, Long> {

    List<BranchTransfer> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    Optional<BranchTransfer> findByIdAndTenant_Id(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select t from BranchTransfer t
        where t.id = :transferId and t.tenant.id = :tenantId
    """)
    Optional<BranchTransfer> findForUpdate(
        @Param("transferId") Long transferId,
        @Param("tenantId") Long tenantId
    );

    @Query("""
        select coalesce(sum(i.quantity), 0L)
        from BranchTransferItem i
        where i.batch.id = :batchId
          and i.transfer.tenant.id = :tenantId
          and i.transfer.status = 'DISPATCHED'
    """)
    Long sumDispatchedQuantityForBatch(
        @Param("tenantId") Long tenantId,
        @Param("batchId") Long batchId
    );
}
