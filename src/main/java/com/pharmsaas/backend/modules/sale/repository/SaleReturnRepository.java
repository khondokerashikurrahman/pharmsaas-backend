package com.pharmsaas.backend.modules.sale.repository;

import com.pharmsaas.backend.modules.sale.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {
    Optional<SaleReturn> findByTenant_IdAndRequestKey(Long tenantId, String requestKey);

    List<SaleReturn> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);
}
