package com.elecciones.election.dto;

import java.util.UUID;

public record PositionResponse(
        UUID id,
        String name
) {
}