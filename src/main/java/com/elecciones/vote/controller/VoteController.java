package com.elecciones.vote.controller;

import com.elecciones.vote.dto.MyVoteStatusResponse;
import com.elecciones.vote.dto.VoteRequest;
import com.elecciones.vote.dto.VoteResponse;
import com.elecciones.vote.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/elections/{electionId}")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/vote")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VOTER')")
    public VoteResponse vote(
            @PathVariable UUID electionId,
            @Valid @RequestBody VoteRequest request
    ) {
        return voteService.vote(electionId, request);
    }

    @GetMapping("/my-status")
    @PreAuthorize("hasRole('VOTER')")
    public MyVoteStatusResponse myStatus(@PathVariable UUID electionId) {
        return voteService.myStatus(electionId);
    }
}