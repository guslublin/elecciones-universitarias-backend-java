package com.elecciones.vote.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VoteRequest(
        @NotNull(message = "El ID de la lista es obligatorio")
        UUID listId
) {
}