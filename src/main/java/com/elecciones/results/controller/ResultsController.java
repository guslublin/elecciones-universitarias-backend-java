package com.elecciones.results.controller;

import com.elecciones.results.dto.ElectionResultsResponse;
import com.elecciones.results.dto.ElectionStatsResponse;
import com.elecciones.results.dto.FinalReportResponse;
import com.elecciones.results.service.ResultsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/elections/{electionId}")
@RequiredArgsConstructor
public class ResultsController {

    private final ResultsService resultsService;

    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'VOTER')")
    public ElectionResultsResponse getResults(@PathVariable UUID electionId) {
        return resultsService.getResults(electionId);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'VOTER')")
    public ElectionStatsResponse getStats(@PathVariable UUID electionId) {
        return resultsService.getStats(electionId);
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public FinalReportResponse getFinalReport(@PathVariable UUID electionId) {
        return resultsService.getFinalReport(electionId);
    }
}