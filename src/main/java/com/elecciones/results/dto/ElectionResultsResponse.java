package com.elecciones.results.dto;

import java.util.List;
import java.util.UUID;

public record ElectionResultsResponse(
        UUID electionId,
        String electionTitle,
        long totalVotes,
        List<ListResultResponse> results,
        UUID winnerListId,
        String winnerListName,
        List<CandidateWinnerResponse> winnersByPosition
) {
}