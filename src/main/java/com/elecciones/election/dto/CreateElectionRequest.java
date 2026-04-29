package com.elecciones.election.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateElectionRequest(
        @NotBlank(message = "El título es obligatorio")
        String title,

        String description,

        @NotNull(message = "La fecha de inicio es obligatoria")
        @Future(message = "La fecha de inicio debe ser futura")
        LocalDateTime startDate,

        @NotNull(message = "La fecha de cierre es obligatoria")
        @Future(message = "La fecha de cierre debe ser futura")
        LocalDateTime endDate
) {
}