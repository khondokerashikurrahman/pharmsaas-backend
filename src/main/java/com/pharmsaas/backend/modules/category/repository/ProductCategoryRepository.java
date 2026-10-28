package com.pharmsaas.backend.modules.category.repository;

import com.pharmsaas.backend.modules.category.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository
        extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByTenant_IdAndActiveTrueOrderByProductTypeAscNameAsc(
        Long tenantId
    );

    List<ProductCategory> findByTenant_IdAndProductTypeIgnoreCaseAndActiveTrueOrderByNameAsc(
        Long tenantId, String productType
    );

    Optional<ProductCategory> findByIdAndTenant_IdAndActiveTrue(
        Long id, Long tenantId
    );

    boolean existsByTenant_Id(Long tenantId);

    boolean existsByTenant_IdAndProductTypeIgnoreCaseAndNameIgnoreCase(
        Long tenantId, String productType, String name
    );

    boolean existsByTenant_IdAndProductTypeIgnoreCaseAndNameIgnoreCaseAndIdNot(
        Long tenantId, String productType, String name, Long id
    );
}
