package com.pharmsaas.backend.modules.medicine.repository;

import com.pharmsaas.backend.modules.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByMedicineCodeIgnoreCase(String medicineCode);

    boolean existsByMedicineCodeIgnoreCase(String medicineCode);

    boolean existsByMedicineCodeIgnoreCaseAndIdNot(
            String medicineCode,
            Long id
    );

    List<Medicine> findByNameContainingIgnoreCase(String name);

    List<Medicine> findByGenericNameContainingIgnoreCase(String genericName);

    List<Medicine> findByStatusIgnoreCase(String status);
}

