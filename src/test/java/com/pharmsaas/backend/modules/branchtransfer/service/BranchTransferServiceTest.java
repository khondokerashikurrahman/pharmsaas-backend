package com.pharmsaas.backend.modules.branchtransfer.service;

import com.pharmsaas.backend.modules.branchtransfer.dto.BranchTransferActionRequest;
import com.pharmsaas.backend.modules.branchtransfer.dto.BranchTransferCreateRequest;
import com.pharmsaas.backend.modules.branchtransfer.entity.BranchTransfer;
import com.pharmsaas.backend.modules.branchtransfer.repository.BranchTransferRepository;
import com.pharmsaas.backend.modules.inventory.repository.StoreInventoryStockRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryRepository;
import com.pharmsaas.backend.modules.inventory.service.InventoryMovementService;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BranchTransferServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final Long TRANSFER_ID = 100L;

    @Mock BranchTransferRepository transferRepository;
    @Mock TenantRepository tenantRepository;
    @Mock TenantMembershipRepository membershipRepository;
    @Mock StoreRepository storeRepository;
    @Mock StoreInventoryStockRepository storeStockRepository;
    @Mock TenantInventoryRepository inventoryRepository;
    @Mock TenantInventoryBatchRepository batchRepository;
    @Mock InventoryMovementService movementService;

    @Mock Tenant tenant;
    @Mock TenantMembership membership;
    @Mock BranchTransfer transfer;

    private BranchTransferService service;

    @BeforeEach
    void setUp() {
        service = new BranchTransferService(
            transferRepository,
            tenantRepository,
            membershipRepository,
            storeRepository,
            storeStockRepository,
            inventoryRepository,
            batchRepository,
            movementService
        );

        when(tenantRepository.findById(TENANT_ID))
            .thenReturn(Optional.of(tenant));
        when(tenant.getStatus()).thenReturn("ACTIVE");

        when(membershipRepository
            .findByTenant_IdAndUser_IdAndActiveTrue(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(membership));
        when(membership.getRole()).thenReturn(MembershipRole.MANAGER);

        when(transferRepository.findForUpdate(TRANSFER_ID, TENANT_ID))
            .thenReturn(Optional.of(transfer));
        when(transfer.getStatus()).thenReturn("PENDING");
        when(transfer.getRequestedBy()).thenReturn(USER_ID);
    }

    @Test
    void shouldRejectApprovalOfOwnTransferRequest() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.approve(
                TENANT_ID, TRANSFER_ID, USER_ID,
                new BranchTransferActionRequest("Approve", null)
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(transferRepository, never()).save(any(BranchTransfer.class));
    }

    @Test
    void shouldRejectRejectionOfOwnTransferRequest() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.reject(
                TENANT_ID, TRANSFER_ID, USER_ID,
                new BranchTransferActionRequest("Reject", null)
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(transferRepository, never()).save(any(BranchTransfer.class));
    }

    @Test
    void shouldRejectApprovalWhenTransferIsNotPending() {
        when(transfer.getStatus()).thenReturn("DISPATCHED");
        when(transfer.getRequestedBy()).thenReturn(99L);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.approve(
                TENANT_ID, TRANSFER_ID, USER_ID, null
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(transferRepository, never()).save(any(BranchTransfer.class));
    }

    @Test
    void shouldRejectDispatchWhenTransferIsNotApproved() {
        when(transfer.getStatus()).thenReturn("PENDING");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.dispatch(TENANT_ID, TRANSFER_ID, USER_ID)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(storeStockRepository, never())
            .findForUpdate(anyLong(), anyLong(), anyLong());
        verify(movementService, never()).record(
            any(), any(), any(), anyString(),
            anyInt(), anyInt(), anyString(),
            any(), anyString(), anyString(), any(), any()
        );
        verify(transferRepository, never()).save(any(BranchTransfer.class));
    }

    @Test
    void shouldRejectReceiveWhenTransferIsNotDispatched() {
        when(transfer.getStatus()).thenReturn("APPROVED");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.receive(
                TENANT_ID, TRANSFER_ID, USER_ID,
                new BranchTransferActionRequest("Receipt", List.of())
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(storeStockRepository, never())
            .findForUpdate(anyLong(), anyLong(), anyLong());
        verify(transferRepository, never()).save(any(BranchTransfer.class));
    }

    @Test
    void shouldRejectApprovalByStoreKeeper() {
        when(membership.getRole()).thenReturn(MembershipRole.STORE_KEEPER);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.approve(
                TENANT_ID, TRANSFER_ID, USER_ID, null
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(transferRepository, never())
            .findForUpdate(TRANSFER_ID, TENANT_ID);
        verify(transferRepository, never()).save(any(BranchTransfer.class));
    }

    @Test
    void shouldRejectCreateWhenSourceAndDestinationAreSameStore() {
        BranchTransferCreateRequest request = new BranchTransferCreateRequest(
            50L,
            50L,
            "Internal transfer",
            List.of(new BranchTransferCreateRequest.Item(20L, 2))
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(TENANT_ID, USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(storeRepository, never())
            .findByIdAndTenant_Id(anyLong(), anyLong());
        verify(transferRepository, never()).save(any(BranchTransfer.class));
    }

    @Test
    void shouldRejectTransferWhenTenantIsNotActive() {
        when(tenant.getStatus()).thenReturn("PENDING");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.dispatch(TENANT_ID, TRANSFER_ID, USER_ID)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(membershipRepository, never())
            .findByTenant_IdAndUser_IdAndActiveTrue(TENANT_ID, USER_ID);
        verify(transferRepository, never())
            .findForUpdate(TRANSFER_ID, TENANT_ID);
    }
}
