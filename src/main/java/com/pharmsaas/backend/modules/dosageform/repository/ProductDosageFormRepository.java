package com.pharmsaas.backend.modules.dosageform.repository;

import com.pharmsaas.backend.modules.dosageform.entity.ProductDosageForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductDosageFormRepository
    extends JpaRepository<ProductDosageForm, Long> {

    List<ProductDosageForm> findByTenant_IdAndActiveTrueOrderByNameAsc(Long tenantId);

    Optional<ProductDosageForm> findByIdAndTenant_IdAndActiveTrue(
        Long id, Long tenantId
    );

    boolean existsByTenant_Id(Long tenantId);

    boolean existsByTenant_IdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenant_IdAndNameIgnoreCaseAndIdNot(
        Long tenantId, String name, Long id
    );
}
