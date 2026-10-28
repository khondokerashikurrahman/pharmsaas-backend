package com.pharmsaas.backend.modules.company.repository;

import com.pharmsaas.backend.modules.company.entity.ManufacturerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManufacturerCompanyRepository
    extends JpaRepository<ManufacturerCompany, Long> {

    List<ManufacturerCompany> findAllByTenant_IdOrderByNameAsc(Long tenantId);

    Optional<ManufacturerCompany> findByIdAndTenant_Id(
        Long id, Long tenantId
    );

    boolean existsByTenant_IdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenant_IdAndNameIgnoreCaseAndIdNot(
        Long tenantId, String name, Long id
    );

    boolean existsByTenant_IdAndCompanyCode(Long tenantId, String companyCode);
}
