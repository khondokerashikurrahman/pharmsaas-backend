
package com.pharmsaas.backend.modules.medicine.service;

import com.pharmsaas.backend.modules.medicine.dto.MedicineRequest;
import com.pharmsaas.backend.modules.medicine.dto.MedicineResponse;
import com.pharmsaas.backend.modules.medicine.entity.Medicine;
import com.pharmsaas.backend.modules.medicine.repository.MedicineRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    // Create Medicine
    public MedicineResponse createMedicine(MedicineRequest request) {

        String medicineCode = normalizeCode(request.medicineCode());

        if (medicineRepository.existsByMedicineCodeIgnoreCase(medicineCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Medicine code already exists: " + medicineCode
            );
        }

        Medicine medicine = new Medicine();

        applyRequest(medicine, request);
        medicine.setMedicineCode(medicineCode);

        Medicine savedMedicine = medicineRepository.save(medicine);

        return toResponse(savedMedicine);
    }

    // Get All Medicines
    @Transactional(readOnly = true)
    public List<MedicineResponse> getAllMedicines() {

        return medicineRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Get Medicine By ID
    @Transactional(readOnly = true)
    public MedicineResponse getMedicineById(Long id) {

        return toResponse(findMedicineById(id));
    }

    // Search Medicines By Name
    @Transactional(readOnly = true)
    public List<MedicineResponse> searchByName(String name) {

        if (name == null || name.isBlank()) {
            return getAllMedicines();
        }

        return medicineRepository
                .findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Search Medicines By Generic Name
    @Transactional(readOnly = true)
    public List<MedicineResponse> searchByGenericName(String genericName) {

        if (genericName == null || genericName.isBlank()) {
            return getAllMedicines();
        }

        return medicineRepository
                .findByGenericNameContainingIgnoreCase(genericName.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Get Medicines By Status
    @Transactional(readOnly = true)
    public List<MedicineResponse> getMedicinesByStatus(String status) {

        String normalizedStatus = normalizeStatus(status);

        return medicineRepository
                .findByStatusIgnoreCase(normalizedStatus)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Update Medicine
    public MedicineResponse updateMedicine(
            Long id,
            MedicineRequest request
    ) {

        Medicine medicine = findMedicineById(id);

        String medicineCode = normalizeCode(request.medicineCode());

        boolean duplicateCode =
                medicineRepository.existsByMedicineCodeIgnoreCaseAndIdNot(
                        medicineCode,
                        id
                );

        if (duplicateCode) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Medicine code already exists: " + medicineCode
            );
        }

        applyRequest(medicine, request);
        medicine.setMedicineCode(medicineCode);

        Medicine updatedMedicine = medicineRepository.save(medicine);

        return toResponse(updatedMedicine);
    }

    // Delete Medicine
    public void deleteMedicine(Long id) {

        Medicine medicine = findMedicineById(id);

        medicineRepository.delete(medicine);
    }

    // Find Medicine Or Throw Error
    private Medicine findMedicineById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Medicine ID must be a positive number"
            );
        }

        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medicine not found with ID: " + id
                ));
    }

    // Copy Request Data To Entity
    private void applyRequest(
            Medicine medicine,
            MedicineRequest request
    ) {

        medicine.setName(request.name().trim());
        medicine.setGenericName(clean(request.genericName()));
        medicine.setBrandName(clean(request.brandName()));
        medicine.setStrength(clean(request.strength()));
        medicine.setDosageForm(clean(request.dosageForm()));
        medicine.setManufacturer(clean(request.manufacturer()));
        medicine.setBarcode(clean(request.barcode()));
        medicine.setDescription(clean(request.description()));
        medicine.setStatus(normalizeStatus(request.status()));
    }

    // Convert Entity To Response DTO
    private MedicineResponse toResponse(Medicine medicine) {

        return new MedicineResponse(
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getName(),
                medicine.getGenericName(),
                medicine.getBrandName(),
                medicine.getStrength(),
                medicine.getDosageForm(),
                medicine.getManufacturer(),
                medicine.getBarcode(),
                medicine.getDescription(),
                medicine.getStatus(),
                medicine.getCreatedAt(),
                medicine.getUpdatedAt()
        );
    }

    // Normalize Medicine Code
    private String normalizeCode(String code) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Medicine code is required"
            );
        }

        return code.trim().toUpperCase(Locale.ROOT);
    }

    // Normalize Medicine Status
    private String normalizeStatus(String status) {

        if (status == null || status.isBlank()) {
            return "Active";
        }

        String normalized = status.trim();

        if ("Active".equalsIgnoreCase(normalized)) {
            return "Active";
        }

        if ("Inactive".equalsIgnoreCase(normalized)) {
            return "Inactive";
        }

        throw new IllegalArgumentException(
                "Invalid status. Allowed values: Active, Inactive"
        );
    }

    // Convert Blank Optional Fields To Null
    private String clean(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
