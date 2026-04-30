package com.elecciones.audit.controller;

import com.elecciones.audit.dto.AuditLogResponse;
import com.elecciones.audit.dto.AuditVerifyRequest;
import com.elecciones.audit.dto.AuditVerifyResponse;
import com.elecciones.audit.service.AuditService;
import com.elecciones.audit.service.AuditVerifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final AuditVerifyService auditVerifyService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditService.findAll(page, size);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public AuditVerifyResponse verify(@Valid @RequestBody AuditVerifyRequest request) {
        return auditVerifyService.verify(request);
    }
}