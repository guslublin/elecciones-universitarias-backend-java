package com.elecciones.election.dto;

import java.util.List;
import java.util.UUID;

public record ElectionListResponse(
        UUID id,
        String name,
        String description,
        List<CandidateResponse> candidates
) {
}