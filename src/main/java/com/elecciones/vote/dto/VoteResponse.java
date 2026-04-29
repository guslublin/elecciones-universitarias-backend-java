package com.elecciones.vote.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VoteResponse(
        UUID voteId,
        UUID electionId,
        UUID listId,
        LocalDateTime votedAt,
        String message
) {
}