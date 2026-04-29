package com.elecciones.election.dto;

import com.elecciones.common.enums.ElectionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ElectionResponse(
        UUID id,
        String title,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        ElectionStatus status,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
}