package com.elecciones.audit.dto;

import java.util.Map;

public record AuditExportResponse(
        Map<String, Object> payload,
        String signature
) {
}