package com.elecciones.election.service;

import com.elecciones.audit.service.AuditService;
import com.elecciones.common.enums.AuditAction;
import com.elecciones.common.enums.ElectionStatus;
import com.elecciones.common.exception.BusinessException;
import com.elecciones.election.dto.CreateElectionRequest;
import com.elecciones.election.dto.CreatePositionRequest;
import com.elecciones.election.dto.ElectionResponse;
import com.elecciones.election.dto.PositionResponse;
import com.elecciones.election.entity.Election;
import com.elecciones.election.entity.Position;
import com.elecciones.election.repository.ElectionListRepository;
import com.elecciones.election.repository.ElectionRepository;
import com.elecciones.election.repository.PositionRepository;
import com.elecciones.election.validator.ElectionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final PositionRepository positionRepository;
    private final ElectionListRepository electionListRepository;
    private final ElectionValidator electionValidator;
    private final AuditService auditService;

    @Transactional
    public ElectionResponse create(CreateElectionRequest request) {
        electionValidator.validateCreate(request);

        Election election = Election.builder()
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(ElectionStatus.DRAFT)
                .build();

        Election saved = electionRepository.save(election);

        auditService.log(
                null,
                getCurrentActor(),
                AuditAction.ELECTION_CREATED,
                "ELECTION",
                saved.getId(),
                Map.of(
                        "title", saved.getTitle(),
                        "status", saved.getStatus().name(),
                        "startDate", saved.getStartDate().toString(),
                        "endDate", saved.getEndDate().toString()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ElectionResponse> findAll() {
        return electionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ElectionResponse findById(UUID id) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        return toResponse(election);
    }

    @Transactional
    public PositionResponse addPosition(UUID electionId, CreatePositionRequest request) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        if (election.getStatus() == ElectionStatus.ACTIVE || election.getStatus() == ElectionStatus.CLOSED) {
            throw new BusinessException(
                    "No se pueden agregar cargos a una elección activa o cerrada",
                    HttpStatus.CONFLICT
            );
        }

        String normalizedName = request.name().trim();

        if (positionRepository.existsByElectionIdAndNameIgnoreCase(electionId, normalizedName)) {
            throw new BusinessException(
                    "Ya existe un cargo con ese nombre en la elección",
                    HttpStatus.CONFLICT
            );
        }

        Position position = Position.builder()
                .election(election)
                .name(normalizedName)
                .build();

        Position saved = positionRepository.save(position);

        return new PositionResponse(
                saved.getId(),
                saved.getName()
        );
    }

    @Transactional
    public ElectionResponse activate(UUID electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        if (election.getStatus() == ElectionStatus.ACTIVE) {
            throw new BusinessException(
                    "La elección ya está activa",
                    HttpStatus.CONFLICT
            );
        }

        if (election.getStatus() == ElectionStatus.CLOSED) {
            throw new BusinessException(
                    "No se puede activar una elección cerrada",
                    HttpStatus.CONFLICT
            );
        }

        long positionsCount = positionRepository.countByElectionId(electionId);
        long listsCount = electionListRepository.countByElectionId(electionId);

        if (positionsCount < 1) {
            throw new BusinessException(
                    "La elección debe tener al menos un cargo para ser activada",
                    HttpStatus.CONFLICT
            );
        }

        if (listsCount < 1) {
            throw new BusinessException(
                    "La elección debe tener al menos una lista válida para ser activada",
                    HttpStatus.CONFLICT
            );
        }

        election.setStatus(ElectionStatus.ACTIVE);

        Election saved = electionRepository.save(election);

        return toResponse(saved);
    }

    @Transactional
    public ElectionResponse close(UUID electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        if (election.getStatus() == ElectionStatus.CLOSED) {
            throw new BusinessException(
                    "La elección ya está cerrada",
                    HttpStatus.CONFLICT
            );
        }

        election.setStatus(ElectionStatus.CLOSED);
        election.setClosedAt(LocalDateTime.now());

        Election saved = electionRepository.save(election);

        auditService.log(
                null,
                getCurrentActor(),
                AuditAction.ELECTION_CLOSED,
                "ELECTION",
                saved.getId(),
                Map.of(
                        "title", saved.getTitle(),
                        "status", saved.getStatus().name(),
                        "closedAt", saved.getClosedAt().toString()
                )
        );

        return toResponse(saved);
    }

    private String getCurrentActor() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ElectionResponse toResponse(Election election) {
        return new ElectionResponse(
                election.getId(),
                election.getTitle(),
                election.getDescription(),
                election.getStartDate(),
                election.getEndDate(),
                election.getStatus(),
                election.getCreatedAt(),
                election.getClosedAt()
        );
    }
}