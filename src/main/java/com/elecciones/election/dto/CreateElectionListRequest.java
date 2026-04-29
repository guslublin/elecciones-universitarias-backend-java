package com.elecciones.election.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateElectionListRequest(
        @NotBlank(message = "El nombre de la lista es obligatorio")
        String name,

        String description,

        @NotEmpty(message = "La lista debe incluir candidatos")
        List<@Valid CandidateRequest> candidates
) {
}