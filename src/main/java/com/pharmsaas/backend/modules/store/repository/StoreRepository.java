package com.pharmsaas.backend.modules.store.repository;

import com.pharmsaas.backend.modules.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByTenant_Id(Long tenantId);

    List<Store> findByTenant_IdAndStatus(Long tenantId, String status);

    Optional<Store> findByIdAndTenant_Id(Long id, Long tenantId);

    boolean existsByTenant_IdAndStoreCode(
            Long tenantId,
            String storeCode
    );

    boolean existsByTenant_IdAndStoreCodeAndIdNot(
            Long tenantId,
            String storeCode,
            Long id
    );
}

