package com.elecciones.results.dto;

import java.util.UUID;

public record ElectionStatsResponse(
        UUID electionId,
        String electionTitle,
        long totalEligibleVoters,
        long votesCast,
        double participationPercentage
) {
}