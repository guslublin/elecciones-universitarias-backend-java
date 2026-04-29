package com.elecciones.election.service;

import com.elecciones.common.enums.ElectionStatus;
import com.elecciones.common.exception.BusinessException;
import com.elecciones.election.dto.CreateElectionRequest;
import com.elecciones.election.dto.ElectionResponse;
import com.elecciones.election.entity.Election;
import com.elecciones.election.repository.ElectionRepository;
import com.elecciones.election.validator.ElectionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final ElectionValidator electionValidator;

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