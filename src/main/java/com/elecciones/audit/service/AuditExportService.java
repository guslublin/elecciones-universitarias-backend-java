package com.elecciones.audit.service;

import com.elecciones.audit.dto.AuditExportResponse;
import com.elecciones.audit.entity.AuditLog;
import com.elecciones.audit.repository.AuditLogRepository;
import com.elecciones.common.enums.AuditAction;
import com.elecciones.common.exception.BusinessException;
import com.elecciones.election.entity.Election;
import com.elecciones.election.repository.ElectionRepository;
import com.elecciones.results.dto.ElectionResultsResponse;
import com.elecciones.results.dto.ElectionStatsResponse;
import com.elecciones.results.dto.ListResultResponse;
import com.elecciones.results.service.ResultsService;
import com.elecciones.security.HmacService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditExportService {

    private final ElectionRepository electionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final ResultsService resultsService;
    private final HmacService hmacService;

    public AuditExportResponse exportElectionAudit(UUID electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        String actor = getCurrentActor();

        auditService.log(
                null,
                actor,
                AuditAction.EXPORT_REQUESTED,
                "ELECTION",
                election.getId(),
                Map.of(
                        "title", election.getTitle(),
                        "exportType", "ELECTION_AUDIT"
                )
        );

        List<AuditLog> events = auditLogRepository.findElectionAuditEvents(electionId);

        ElectionResultsResponse results = resultsService.getResults(electionId);
        ElectionStatsResponse stats = resultsService.getStats(electionId);

        Map<String, Object> payload = Map.of(
                "election_id", election.getId().toString(),
                "election_title", election.getTitle(),
                "generated_at", Instant.now().toString(),
                "generated_by", actor,
                "events", events.stream().map(this::toEventMap).toList(),
                "summary", Map.of(
                        "total_voters", stats.totalEligibleVoters(),
                        "votes_cast", stats.votesCast(),
                        "participation_pct", stats.participationPercentage(),
                        "results", results.results().stream().map(this::toResultMap).toList()
                )
        );

        String signature = hmacService.signPayload(payload);

        return new AuditExportResponse(payload, signature);
    }

    private Map<String, Object> toEventMap(AuditLog auditLog) {
        return Map.of(
                "id", auditLog.getId().toString(),
                "actor", auditLog.getActor(),
                "action", auditLog.getAction().name(),
                "entity_type", auditLog.getEntityType(),
                "entity_id", auditLog.getEntityId() == null ? "" : auditLog.getEntityId().toString(),
                "detail", auditLog.getDetail(),
                "timestamp", auditLog.getCreatedAt().toString()
        );
    }

    private Map<String, Object> toResultMap(ListResultResponse result) {
        return Map.of(
                "list", result.listName(),
                "votes", result.votes(),
                "pct", result.percentage()
        );
    }

    private String getCurrentActor() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}