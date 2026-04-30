package com.elecciones.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AuditVerifyRequest(
        @NotNull(message = "El payload es obligatorio")
        Map<String, Object> payload,

        @NotBlank(message = "La firma es obligatoria")
        String signature
) {
}