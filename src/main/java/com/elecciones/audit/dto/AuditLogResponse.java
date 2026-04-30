package com.elecciones.audit.dto;

import com.elecciones.common.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actor,
        AuditAction action,
        String entityType,
        UUID entityId,
        Map<String, Object> detail,
        LocalDateTime createdAt
) {
}