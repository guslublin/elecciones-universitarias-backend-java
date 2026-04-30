package com.elecciones.vote.service;

import com.elecciones.audit.service.AuditService;
import com.elecciones.common.enums.AuditAction;
import com.elecciones.common.enums.ElectionStatus;
import com.elecciones.common.exception.BusinessException;
import com.elecciones.common.util.HashService;
import com.elecciones.election.entity.Election;
import com.elecciones.election.entity.ElectionList;
import com.elecciones.election.repository.ElectionListRepository;
import com.elecciones.election.repository.ElectionRepository;
import com.elecciones.user.entity.User;
import com.elecciones.user.repository.UserRepository;
import com.elecciones.vote.dto.MyVoteStatusResponse;
import com.elecciones.vote.dto.VoteRequest;
import com.elecciones.vote.dto.VoteResponse;
import com.elecciones.vote.entity.Vote;
import com.elecciones.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final ElectionRepository electionRepository;
    private final ElectionListRepository electionListRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final HashService hashService;
    private final AuditService auditService;

    @Transactional
    public VoteResponse vote(UUID electionId, VoteRequest request) {
        User currentUser = getCurrentUser();

        Election election = electionRepository.findByIdForUpdate(electionId)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        if (election.getStatus() != ElectionStatus.ACTIVE) {
            throw new BusinessException(
                    "Solo se puede votar en elecciones activas",
                    HttpStatus.CONFLICT
            );
        }

        ElectionList electionList = electionListRepository.findByIdAndElectionId(request.listId(), electionId)
                .orElseThrow(() -> new BusinessException(
                        "La lista seleccionada no pertenece a esta elección",
                        HttpStatus.BAD_REQUEST
                ));

        String voterHash = buildVoterHash(currentUser.getId(), electionId);

        if (voteRepository.existsByVoterHashAndElectionId(voterHash, electionId)) {
            throw new BusinessException(
                    "El usuario ya emitió su voto en esta elección",
                    HttpStatus.CONFLICT
            );
        }

        try {
            Vote vote = Vote.builder()
                    .election(election)
                    .electionList(electionList)
                    .voterHash(voterHash)
                    .build();

            Vote savedVote = voteRepository.saveAndFlush(vote);

            saveVoteAudit(savedVote, election, electionList, voterHash);

            return new VoteResponse(
                    savedVote.getId(),
                    election.getId(),
                    electionList.getId(),
                    savedVote.getVotedAt(),
                    "Voto registrado correctamente"
            );
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    "El usuario ya emitió su voto en esta elección",
                    HttpStatus.CONFLICT
            );
        }
    }

    @Transactional(readOnly = true)
    public MyVoteStatusResponse myStatus(UUID electionId) {
        User currentUser = getCurrentUser();

        if (!electionRepository.existsById(electionId)) {
            throw new BusinessException(
                    "Elección no encontrada",
                    HttpStatus.NOT_FOUND
            );
        }

        String voterHash = buildVoterHash(currentUser.getId(), electionId);
        boolean voted = voteRepository.existsByVoterHashAndElectionId(voterHash, electionId);

        return new MyVoteStatusResponse(electionId, voted);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "Usuario autenticado no encontrado",
                        HttpStatus.UNAUTHORIZED
                ));
    }

    private String buildVoterHash(UUID userId, UUID electionId) {
        return hashService.hashVoter(userId + ":" + electionId);
    }

    private void saveVoteAudit(Vote vote, Election election, ElectionList electionList, String voterHash) {
        String anonymousActor = "anonymous:" + voterHash.substring(0, 12);

        auditService.log(
                null,
                anonymousActor,
                AuditAction.VOTE_CAST,
                "VOTE",
                vote.getId(),
                Map.of(
                        "electionId", election.getId().toString(),
                        "electionTitle", election.getTitle(),
                        "listId", electionList.getId().toString(),
                        "listName", electionList.getName()
                )
        );
    }
}