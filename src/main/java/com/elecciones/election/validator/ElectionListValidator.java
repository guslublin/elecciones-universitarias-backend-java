package com.elecciones.election.validator;

import com.elecciones.common.enums.ElectionStatus;
import com.elecciones.common.exception.BusinessException;
import com.elecciones.election.dto.CandidateRequest;
import com.elecciones.election.dto.CreateElectionListRequest;
import com.elecciones.election.entity.Election;
import com.elecciones.election.entity.Position;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ElectionListValidator {

    public void validateCanCreateList(Election election) {
        if (election.getStatus() == ElectionStatus.ACTIVE || election.getStatus() == ElectionStatus.CLOSED) {
            throw new BusinessException(
                    "No se pueden registrar listas en una elección activa o cerrada",
                    HttpStatus.CONFLICT
            );
        }
    }

    public void validateCandidatesMatchPositions(
            CreateElectionListRequest request,
            List<Position> electionPositions
    ) {
        if (electionPositions.isEmpty()) {
            throw new BusinessException(
                    "La elección debe tener cargos definidos antes de registrar listas",
                    HttpStatus.CONFLICT
            );
        }

        List<CandidateRequest> candidates = request.candidates();

        if (candidates.size() != electionPositions.size()) {
            throw new BusinessException(
                    "La lista debe incluir exactamente un candidato por cada cargo",
                    HttpStatus.BAD_REQUEST
            );
        }

        Set<UUID> receivedPositionIds = candidates.stream()
                .map(CandidateRequest::positionId)
                .collect(Collectors.toSet());

        if (receivedPositionIds.size() != candidates.size()) {
            throw new BusinessException(
                    "No se permiten cargos repetidos en la lista",
                    HttpStatus.BAD_REQUEST
            );
        }

        Set<UUID> validPositionIds = electionPositions.stream()
                .map(Position::getId)
                .collect(Collectors.toSet());

        if (!validPositionIds.equals(receivedPositionIds)) {
            throw new BusinessException(
                    "Los candidatos deben corresponder exactamente a los cargos definidos en la elección",
                    HttpStatus.BAD_REQUEST
            );
        }

        Set<String> candidateNames = new HashSet<>();

        for (CandidateRequest candidate : candidates) {
            String normalizedName = candidate.fullName().trim().toLowerCase();

            if (!candidateNames.add(normalizedName)) {
                throw new BusinessException(
                        "No se permiten candidatos repetidos dentro de la misma lista",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
    }
}