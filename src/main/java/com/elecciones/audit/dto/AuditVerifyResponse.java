package com.elecciones.audit.dto;

public record AuditVerifyResponse(
        boolean valid,
        String message
) {
}