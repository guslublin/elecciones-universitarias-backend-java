package com.elecciones.election.dto;

import java.util.UUID;

public record CandidateResponse(
        UUID id,
        UUID positionId,
        String positionName,
        String fullName,
        String career,
        String proposal
) {
}