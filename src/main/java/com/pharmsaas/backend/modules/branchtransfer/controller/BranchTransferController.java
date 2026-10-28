package com.pharmsaas.backend.modules.branchtransfer.controller;

import com.pharmsaas.backend.modules.branchtransfer.dto.BranchTransferActionRequest;
import com.pharmsaas.backend.modules.branchtransfer.dto.BranchTransferCreateRequest;
import com.pharmsaas.backend.modules.branchtransfer.dto.BranchTransferResponse;
import com.pharmsaas.backend.modules.branchtransfer.service.BranchTransferService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/branch-transfers")
public class BranchTransferController {

    private final BranchTransferService service;

    public BranchTransferController(BranchTransferService service) {
        this.service = service;
    }

    private Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (Exception exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }
    }

    @GetMapping
    public List<BranchTransferResponse> list(
            @PathVariable Long tenantId,
            Authentication authentication) {
        return service.list(tenantId, currentUserId(authentication));
    }

    @GetMapping("/{transferId}")
    public BranchTransferResponse get(
            @PathVariable Long tenantId,
            @PathVariable Long transferId,
            Authentication authentication) {
        return service.get(
            tenantId, transferId, currentUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchTransferResponse create(
            @PathVariable Long tenantId,
            @RequestBody BranchTransferCreateRequest request,
            Authentication authentication) {
        return service.create(
            tenantId, currentUserId(authentication), request);
    }

    @PostMapping("/{transferId}/approve")
    public BranchTransferResponse approve(
            @PathVariable Long tenantId,
            @PathVariable Long transferId,
            @RequestBody(required = false) BranchTransferActionRequest request,
            Authentication authentication) {
        return service.approve(
            tenantId, transferId, currentUserId(authentication), request);
    }

    @PostMapping("/{transferId}/reject")
    public BranchTransferResponse reject(
            @PathVariable Long tenantId,
            @PathVariable Long transferId,
            @RequestBody(required = false) BranchTransferActionRequest request,
            Authentication authentication) {
        return service.reject(
            tenantId, transferId, currentUserId(authentication), request);
    }

    @PostMapping("/{transferId}/dispatch")
    public BranchTransferResponse dispatch(
            @PathVariable Long tenantId,
            @PathVariable Long transferId,
            Authentication authentication) {
        return service.dispatch(
            tenantId, transferId, currentUserId(authentication));
    }

    @PostMapping("/{transferId}/receive")
    public BranchTransferResponse receive(
            @PathVariable Long tenantId,
            @PathVariable Long transferId,
            @RequestBody BranchTransferActionRequest request,
            Authentication authentication) {
        return service.receive(
            tenantId, transferId, currentUserId(authentication), request);
    }
}
