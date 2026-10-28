package com.pharmsaas.backend.modules.supplier.repository;

import com.pharmsaas.backend.modules.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByTenant_IdOrderByNameAsc(Long tenantId);

    Optional<Supplier> findByIdAndTenant_Id(Long id, Long tenantId);

    Optional<Supplier> findByTenant_IdAndNameIgnoreCase(
        Long tenantId, String name
    );

    boolean existsByTenant_IdAndSupplierCode(
        Long tenantId, String supplierCode
    );
}
