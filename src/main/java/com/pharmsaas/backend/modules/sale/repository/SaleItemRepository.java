package com.pharmsaas.backend.modules.sale.repository;

import com.pharmsaas.backend.modules.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findAllBySale_IdOrderByIdAsc(Long saleId);
}
