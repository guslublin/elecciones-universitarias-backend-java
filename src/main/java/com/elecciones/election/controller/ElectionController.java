package com.elecciones.election.controller;

import com.elecciones.election.dto.CreateElectionRequest;
import com.elecciones.election.dto.CreatePositionRequest;
import com.elecciones.election.dto.ElectionResponse;
import com.elecciones.election.dto.PositionResponse;
import com.elecciones.election.service.ElectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/elections")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionService electionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ElectionResponse create(@Valid @RequestBody CreateElectionRequest request) {
        return electionService.create(request);
    }

    @GetMapping
    public List<ElectionResponse> findAll() {
        return electionService.findAll();
    }

    @GetMapping("/{id}")
    public ElectionResponse findById(@PathVariable UUID id) {
        return electionService.findById(id);
    }

    @PostMapping("/{id}/positions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PositionResponse addPosition(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePositionRequest request
    ) {
        return electionService.addPosition(id, request);
    }
}