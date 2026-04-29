package com.elecciones.results.dto;

public record FinalReportResponse(
        ElectionResultsResponse results,
        ElectionStatsResponse stats
) {
}