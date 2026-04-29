package com.elecciones.election.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
        @NotBlank(message = "El nombre del cargo es obligatorio")
        @Size(max = 120, message = "El nombre del cargo no puede superar 120 caracteres")
        String name
) {
}