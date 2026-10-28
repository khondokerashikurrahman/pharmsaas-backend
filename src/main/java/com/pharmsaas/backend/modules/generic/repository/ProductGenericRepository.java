package com.pharmsaas.backend.modules.generic.repository;

import com.pharmsaas.backend.modules.generic.entity.ProductGeneric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductGenericRepository extends JpaRepository<ProductGeneric, Long> {
    List<ProductGeneric> findByTenant_IdAndActiveTrueOrderByNameAsc(Long tenantId);

    Optional<ProductGeneric> findByIdAndTenant_IdAndActiveTrue(
        Long id, Long tenantId
    );

    boolean existsByTenant_IdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenant_IdAndNameIgnoreCaseAndIdNot(
        Long tenantId, String name, Long id
    );
}
