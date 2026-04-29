package com.elecciones.results.dto;

import java.util.UUID;

public record CandidateWinnerResponse(
        UUID positionId,
        String positionName,
        UUID candidateId,
        String fullName,
        String career,
        String proposal
) {
}