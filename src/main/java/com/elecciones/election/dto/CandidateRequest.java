package com.elecciones.election.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CandidateRequest(
        @NotNull(message = "El cargo del candidato es obligatorio")
        UUID positionId,

        @NotBlank(message = "El nombre completo del candidato es obligatorio")
        String fullName,

        String career,

        String proposal
) {
}