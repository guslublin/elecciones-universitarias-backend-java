package com.elecciones.audit.controller;

import com.elecciones.audit.dto.AuditLogResponse;
import com.elecciones.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogResponse> findAll(Pageable pageable) {
        return auditService.findAll(pageable);
    }
}