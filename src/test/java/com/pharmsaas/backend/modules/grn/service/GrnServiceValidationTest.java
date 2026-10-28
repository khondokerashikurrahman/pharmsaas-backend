package com.pharmsaas.backend.modules.grn.service;

import com.pharmsaas.backend.modules.grn.dto.GrnCreateRequest;
import com.pharmsaas.backend.modules.grn.entity.GoodsReceivedNote;
import com.pharmsaas.backend.modules.grn.entity.GoodsReceivedNoteItem;
import com.pharmsaas.backend.modules.grn.repository.GoodsReceivedNoteRepository;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.repository.StoreInventoryStockRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
import com.pharmsaas.backend.modules.inventory.service.InventoryBatchService;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrder;
import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrderItem;
import com.pharmsaas.backend.modules.purchaseorder.entity.PurchaseOrderStatus;
import com.pharmsaas.backend.modules.purchaseorder.repository.PurchaseOrderRepository;
import com.pharmsaas.backend.modules.store.entity.Store;
import com.pharmsaas.backend.modules.store.repository.StoreRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrnServiceValidationTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 50L;
    private static final Long MEDICINE_ID = 11L;
    private static final String PO_NUMBER = "PO-TEST-001";

    @Mock GoodsReceivedNoteRepository grnRepository;
    @Mock InventoryBatchService batchService;
    @Mock TenantRepository tenantRepository;
    @Mock TenantMembershipRepository membershipRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock StoreRepository storeRepository;
    @Mock TenantInventoryBatchRepository batchRepository;
    @Mock StoreInventoryStockRepository storeStockRepository;

    @Mock Tenant tenant;
    @Mock TenantMembership membership;
    @Mock PurchaseOrder order;
    @Mock PurchaseOrderItem orderedLine;
    @Mock Store store;

    private GrnService service;

    @BeforeEach
    void setUp() {
        service = new GrnService(
            grnRepository,
            batchService,
            tenantRepository,
            membershipRepository,
            purchaseOrderRepository,
            storeRepository,
            batchRepository,
            storeStockRepository
        );

        when(tenantRepository.findById(TENANT_ID))
            .thenReturn(Optional.of(tenant));
        when(tenant.getStatus()).thenReturn("ACTIVE");

        when(membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(membership));
        when(membership.getRole()).thenReturn(MembershipRole.MANAGER);

        when(storeRepository.findByIdAndTenant_Id(STORE_ID, TENANT_ID))
            .thenReturn(Optional.of(store));
        when(store.getStatus()).thenReturn("ACTIVE");

        when(grnRepository
            .existsByTenant_IdAndSupplierInvoiceNumberIgnoreCase(
                TENANT_ID, "INV-TEST-001"
            )).thenReturn(false);

        when(purchaseOrderRepository
            .findForUpdateByTenantAndOrderNumber(TENANT_ID, PO_NUMBER))
            .thenReturn(Optional.of(order));

        when(order.getStatus()).thenReturn(PurchaseOrderStatus.PENDING);
        when(order.getSupplierName()).thenReturn("Test Supplier");
        when(order.getItems()).thenReturn(List.of(orderedLine));

        when(orderedLine.getInventoryItemId()).thenReturn(MEDICINE_ID);
        when(orderedLine.getQuantity()).thenReturn(5);

        when(grnRepository.findAllByTenant_IdOrderByCreatedAtDesc(TENANT_ID))
            .thenReturn(List.of());
    }

    private GrnCreateRequest request(int quantity, Long storeId) {
        LocalDate today = LocalDate.now();

        return new GrnCreateRequest(
            "Test Supplier",
            PO_NUMBER,
            "INV-TEST-001",
            today,
            "Automated validation test",
            List.of(new GrnCreateRequest.Item(
                MEDICINE_ID,
                "BATCH-TEST-001",
                today.minusDays(30),
                today.plusDays(300),
                quantity,
                new BigDecimal("10.00"),
                new BigDecimal("15.00")
            )),
            storeId
        );
    }

    @Test
    void shouldRejectReceiptExceedingOrderedQuantity() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(
                TENANT_ID, USER_ID, request(6, STORE_ID)
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(purchaseOrderRepository).findForUpdateByTenantAndOrderNumber(
            TENANT_ID, PO_NUMBER
        );
        verify(batchService, never()).createForGrn(
            anyLong(), anyLong(), any(), anyLong(), anyString(), any(Store.class)
        );
        verify(storeStockRepository, never()).save(any());
    }

    @Test
    void shouldRejectReceiptExceedingRemainingQuantityAfterPreviousGrn() {
        GoodsReceivedNote existingGrn = mock(GoodsReceivedNote.class);
        GoodsReceivedNoteItem existingLine = mock(GoodsReceivedNoteItem.class);

        when(existingGrn.getPurchaseOrderNumber()).thenReturn(PO_NUMBER);
        when(existingGrn.getItems()).thenReturn(List.of(existingLine));
        when(existingLine.getInventoryItemId()).thenReturn(MEDICINE_ID);
        when(existingLine.getQuantity()).thenReturn(3);

        when(grnRepository.findAllByTenant_IdOrderByCreatedAtDesc(TENANT_ID))
            .thenReturn(List.of(existingGrn));

        // Ordered 5, previously received 3, so receiving 3 more must fail.
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(
                TENANT_ID, USER_ID, request(3, STORE_ID)
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(batchService, never()).createForGrn(
            anyLong(), anyLong(), any(), anyLong(), anyString(), any(Store.class)
        );
        verify(storeStockRepository, never()).save(any());
    }

    @Test
    void shouldRejectGrnWithoutReceivingStore() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(
                TENANT_ID, USER_ID, request(2, null)
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(storeRepository, never())
            .findByIdAndTenant_Id(anyLong(), anyLong());
        verify(purchaseOrderRepository, never())
            .findForUpdateByTenantAndOrderNumber(anyLong(), anyString());
        verify(batchService, never()).createForGrn(
            anyLong(), anyLong(), any(), anyLong(), anyString(), any(Store.class)
        );
    }
}
