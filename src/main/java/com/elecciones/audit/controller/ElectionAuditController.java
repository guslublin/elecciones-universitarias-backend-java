package com.elecciones.audit.controller;

import com.elecciones.audit.dto.AuditExportResponse;
import com.elecciones.audit.service.AuditExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/elections/{electionId}/audit")
@RequiredArgsConstructor
public class ElectionAuditController {

    private final AuditExportService auditExportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public AuditExportResponse exportElectionAudit(@PathVariable UUID electionId) {
        return auditExportService.exportElectionAudit(electionId);
    }
}