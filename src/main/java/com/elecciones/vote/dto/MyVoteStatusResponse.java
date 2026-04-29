package com.elecciones.vote.dto;

import java.util.UUID;

public record MyVoteStatusResponse(
        UUID electionId,
        boolean voted
) {
}