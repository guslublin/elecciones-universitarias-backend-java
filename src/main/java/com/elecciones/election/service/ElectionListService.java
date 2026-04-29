package com.elecciones.election.service;

import com.elecciones.common.exception.BusinessException;
import com.elecciones.election.dto.CandidateRequest;
import com.elecciones.election.dto.CandidateResponse;
import com.elecciones.election.dto.CreateElectionListRequest;
import com.elecciones.election.dto.ElectionListResponse;
import com.elecciones.election.entity.Candidate;
import com.elecciones.election.entity.Election;
import com.elecciones.election.entity.ElectionList;
import com.elecciones.election.entity.Position;
import com.elecciones.election.repository.CandidateRepository;
import com.elecciones.election.repository.ElectionListRepository;
import com.elecciones.election.repository.ElectionRepository;
import com.elecciones.election.repository.PositionRepository;
import com.elecciones.election.validator.ElectionListValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElectionListService {

    private final ElectionRepository electionRepository;
    private final PositionRepository positionRepository;
    private final ElectionListRepository electionListRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionListValidator electionListValidator;

    @Transactional
    public ElectionListResponse createList(UUID electionId, CreateElectionListRequest request) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        electionListValidator.validateCanCreateList(election);

        String normalizedListName = request.name().trim();

        if (electionListRepository.existsByElectionIdAndNameIgnoreCase(electionId, normalizedListName)) {
            throw new BusinessException(
                    "Ya existe una lista con ese nombre en la elección",
                    HttpStatus.CONFLICT
            );
        }

        List<Position> positions = positionRepository.findByElectionId(electionId);

        electionListValidator.validateCandidatesMatchPositions(request, positions);

        Map<UUID, Position> positionMap = positions.stream()
                .collect(Collectors.toMap(Position::getId, position -> position));

        ElectionList electionList = ElectionList.builder()
                .election(election)
                .name(normalizedListName)
                .description(request.description())
                .build();

        ElectionList savedList = electionListRepository.save(electionList);

        List<Candidate> candidates = request.candidates()
                .stream()
                .map(candidateRequest -> buildCandidate(candidateRequest, savedList, positionMap))
                .toList();

        List<Candidate> savedCandidates = candidateRepository.saveAll(candidates);

        return toResponse(savedList, savedCandidates);
    }

    private Candidate buildCandidate(
            CandidateRequest request,
            ElectionList savedList,
            Map<UUID, Position> positionMap
    ) {
        Position position = positionMap.get(request.positionId());

        return Candidate.builder()
                .electionList(savedList)
                .position(position)
                .fullName(request.fullName().trim())
                .career(request.career())
                .proposal(request.proposal())
                .build();
    }

    private ElectionListResponse toResponse(ElectionList list, List<Candidate> candidates) {
        List<CandidateResponse> candidateResponses = candidates.stream()
                .sorted(Comparator.comparing(candidate -> candidate.getPosition().getName()))
                .map(candidate -> new CandidateResponse(
                        candidate.getId(),
                        candidate.getPosition().getId(),
                        candidate.getPosition().getName(),
                        candidate.getFullName(),
                        candidate.getCareer(),
                        candidate.getProposal()
                ))
                .toList();

        return new ElectionListResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                candidateResponses
        );
    }
}