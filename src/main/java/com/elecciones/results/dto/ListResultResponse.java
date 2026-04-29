package com.elecciones.results.dto;

import java.util.UUID;

public record ListResultResponse(
        UUID listId,
        String listName,
        long votes,
        double percentage
) {
}