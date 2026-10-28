package com.pharmsaas.backend.modules.inventory.service;

import com.pharmsaas.backend.modules.inventory.dto.StockAdjustmentRequest;
import com.pharmsaas.backend.modules.inventory.entity.InventoryMovement;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryRepository;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockAdjustmentServiceTest {

    @Mock TenantInventoryBatchRepository batchRepository;
    @Mock TenantInventoryRepository inventoryRepository;
    @Mock TenantMembershipRepository membershipRepository;
    @Mock TenantRepository tenantRepository;
    @Mock InventoryMovementService movementService;

    @Mock Tenant tenant;
    @Mock TenantMembership membership;
    @Mock TenantInventoryItem item;
    @Mock TenantInventoryBatch batch;
    @Mock InventoryMovement movement;

    private StockAdjustmentService service;

    @BeforeEach
    void setUp() {
        service = new StockAdjustmentService(
            batchRepository,
            inventoryRepository,
            membershipRepository,
            tenantRepository,
            movementService
        );

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenant.getId()).thenReturn(1L);
        when(tenant.getStatus()).thenReturn("ACTIVE");

        when(membershipRepository.findByTenant_IdAndUser_IdAndActiveTrue(1L, 7L))
            .thenReturn(Optional.of(membership));
        when(membership.getRole()).thenReturn(MembershipRole.MANAGER);

        when(batchRepository.findByIdAndTenant_Id(20L, 1L))
            .thenReturn(Optional.of(batch));
        when(batchRepository.findForUpdate(20L, 1L))
            .thenReturn(Optional.of(batch));

        when(batch.getInventoryItem()).thenReturn(item);
        when(batch.getQuantity()).thenReturn(10);
        when(batch.getExpiryDate()).thenReturn(LocalDate.now().plusDays(100));
        when(batch.getStatus()).thenReturn("ACTIVE");
        when(batch.getId()).thenReturn(20L);
        when(batch.getBatchNumber()).thenReturn("BATCH-20");

        when(inventoryRepository.findForUpdate(11L, 1L))
            .thenReturn(Optional.of(item));
        when(item.getId()).thenReturn(11L);
        when(item.getTenant()).thenReturn(tenant);
        when(item.getStatus()).thenReturn("ACTIVE");
        when(item.getName()).thenReturn("Test Medicine");
        when(item.getStockQuantity()).thenReturn(30);

        when(movement.getId()).thenReturn(99L);
        when(movement.getCreatedAt())
            .thenReturn(OffsetDateTime.parse("2026-01-01T10:00:00Z"));

        when(movementService.record(
            any(Tenant.class),
            any(TenantInventoryItem.class),
            any(TenantInventoryBatch.class),
            eq("ADJUSTMENT"),
            anyInt(),
            anyInt(),
            eq("STOCK_ADJUSTMENT"),
            isNull(),
            anyString(),
            anyString(),
            eq(7L)
        )).thenReturn(movement);
    }

    @Test
    void shouldRecordSuccessfulStockIncrease() {
        var response = service.adjust(
            1L, 7L,
            new StockAdjustmentRequest(20L, 5, "Physical count correction")
        );

        assertEquals(99L, response.movementId());
        assertEquals(15, response.batchQuantityAfter());
        assertEquals(35, response.inventoryQuantityAfter());

        verify(batchRepository).save(batch);
        verify(inventoryRepository).save(item);
        verify(movementService).record(
            tenant, item, batch,
            "ADJUSTMENT", 5, 15,
            "STOCK_ADJUSTMENT", null,
            response.adjustmentNumber(),
            "Physical count correction", 7L
        );
    }

    @Test
    void shouldRejectIncreaseIntoExpiredBatch() {
        when(batch.getExpiryDate()).thenReturn(LocalDate.now().minusDays(1));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.adjust(
                1L, 7L,
                new StockAdjustmentRequest(20L, 2, "Stock correction")
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(movementService, never()).record(
            any(), any(), any(), anyString(),
            anyInt(), anyInt(), anyString(),
            any(), anyString(), anyString(), any()
        );
    }

    @Test
    void shouldRejectUserWithoutAdjustmentPermission() {
        when(membership.getRole()).thenReturn(MembershipRole.STORE_KEEPER);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.adjust(
                1L, 7L,
                new StockAdjustmentRequest(20L, 1, "Correction")
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(batchRepository, never()).findForUpdate(anyLong(), anyLong());
        verify(movementService, never()).record(
            any(), any(), any(), anyString(),
            anyInt(), anyInt(), anyString(),
            any(), anyString(), anyString(), any()
        );
    }
}
