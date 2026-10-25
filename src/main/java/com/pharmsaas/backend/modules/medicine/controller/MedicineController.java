package com.pharmsaas.backend.modules.medicine.controller;

import com.pharmsaas.backend.modules.medicine.dto.MedicineRequest;
import com.pharmsaas.backend.modules.medicine.dto.MedicineResponse;
import com.pharmsaas.backend.modules.medicine.service.MedicineService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    // Create Medicine
    @PostMapping
    public ResponseEntity<MedicineResponse> createMedicine(
            @Valid @RequestBody MedicineRequest request
    ) {
        MedicineResponse response =
                medicineService.createMedicine(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get All Medicines
    @GetMapping
    public ResponseEntity<List<MedicineResponse>> getAllMedicines() {

        return ResponseEntity.ok(
                medicineService.getAllMedicines()
        );
    }

    // Get Medicine By ID
    @GetMapping("/{id}")
    public ResponseEntity<MedicineResponse> getMedicineById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                medicineService.getMedicineById(id)
        );
    }

    // Search By Medicine Name
    @GetMapping("/search")
    public ResponseEntity<List<MedicineResponse>> searchByName(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                medicineService.searchByName(name)
        );
    }

    // Search By Generic Name
    @GetMapping("/search/generic")
    public ResponseEntity<List<MedicineResponse>> searchByGenericName(
            @RequestParam String genericName
    ) {
        return ResponseEntity.ok(
                medicineService.searchByGenericName(genericName)
        );
    }

    // Filter By Status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<MedicineResponse>> getByStatus(
            @PathVariable String status
    ) {
        return ResponseEntity.ok(
                medicineService.getMedicinesByStatus(status)
        );
    }

    // Update Medicine
    @PutMapping("/{id}")
    public ResponseEntity<MedicineResponse> updateMedicine(
            @PathVariable Long id,
            @Valid @RequestBody MedicineRequest request
    ) {
        return ResponseEntity.ok(
                medicineService.updateMedicine(id, request)
        );
    }

    // Delete Medicine
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicine(
            @PathVariable Long id
    ) {
        medicineService.deleteMedicine(id);

        return ResponseEntity.noContent().build();
    }
}
