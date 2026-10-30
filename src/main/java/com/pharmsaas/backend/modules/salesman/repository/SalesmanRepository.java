package com.pharmsaas.backend.modules.salesman.repository;

import com.pharmsaas.backend.modules.salesman.entity.Salesman;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesmanRepository extends JpaRepository<Salesman, Long> {

    List<Salesman> findAllByTenant_IdOrderByNameAsc(Long tenantId);

    Optional<Salesman> findByIdAndTenant_Id(Long id, Long tenantId);

    Optional<Salesman> findByTenant_IdAndEmployeeIdIgnoreCase(
        Long tenantId, String employeeId
    );
}
