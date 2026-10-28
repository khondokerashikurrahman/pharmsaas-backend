package com.pharmsaas.backend.modules.inventory.repository;

import com.pharmsaas.backend.modules.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository
    extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);
}
