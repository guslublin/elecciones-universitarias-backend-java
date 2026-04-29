package com.elecciones.results.service;

import com.elecciones.common.enums.ElectionStatus;
import com.elecciones.common.enums.RoleName;
import com.elecciones.common.exception.BusinessException;
import com.elecciones.election.entity.Candidate;
import com.elecciones.election.entity.Election;
import com.elecciones.election.entity.ElectionList;
import com.elecciones.election.repository.ElectionListRepository;
import com.elecciones.election.repository.ElectionRepository;
import com.elecciones.results.dto.*;
import com.elecciones.user.repository.UserRepository;
import com.elecciones.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultsService {

    private final ElectionRepository electionRepository;
    private final ElectionListRepository electionListRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ElectionResultsResponse getResults(UUID electionId) {
        Election election = getElection(electionId);
        List<ElectionList> lists = electionListRepository.findByElectionId(electionId);

        long totalVotes = voteRepository.countByElectionId(electionId);

        List<ListResultResponse> results = lists.stream()
                .map(list -> {
                    long votes = voteRepository.countByElectionListId(list.getId());
                    double percentage = calculatePercentage(votes, totalVotes);

                    return new ListResultResponse(
                            list.getId(),
                            list.getName(),
                            votes,
                            percentage
                    );
                })
                .sorted(Comparator.comparing(ListResultResponse::votes).reversed())
                .toList();

        ListResultResponse winner = results.isEmpty() ? null : results.get(0);

        List<CandidateWinnerResponse> winnersByPosition = winner == null
                ? List.of()
                : lists.stream()
                        .filter(list -> list.getId().equals(winner.listId()))
                        .findFirst()
                        .map(this::mapWinnerCandidates)
                        .orElse(List.of());

        return new ElectionResultsResponse(
                election.getId(),
                election.getTitle(),
                totalVotes,
                results,
                winner == null ? null : winner.listId(),
                winner == null ? null : winner.listName(),
                winnersByPosition
        );
    }

    @Transactional(readOnly = true)
    public ElectionStatsResponse getStats(UUID electionId) {
        Election election = getElection(electionId);

        long totalEligibleVoters = userRepository.countByRole(RoleName.VOTER);
        long votesCast = voteRepository.countByElectionId(electionId);
        double participationPercentage = calculatePercentage(votesCast, totalEligibleVoters);

        return new ElectionStatsResponse(
                election.getId(),
                election.getTitle(),
                totalEligibleVoters,
                votesCast,
                participationPercentage
        );
    }

    @Transactional(readOnly = true)
    public FinalReportResponse getFinalReport(UUID electionId) {
        Election election = getElection(electionId);

        if (election.getStatus() != ElectionStatus.CLOSED) {
            throw new BusinessException(
                    "El reporte final solo está disponible para elecciones cerradas",
                    HttpStatus.CONFLICT
            );
        }

        return new FinalReportResponse(
                getResults(electionId),
                getStats(electionId)
        );
    }

    private Election getElection(UUID electionId) {
        return electionRepository.findById(electionId)
                .orElseThrow(() -> new BusinessException(
                        "Elección no encontrada",
                        HttpStatus.NOT_FOUND
                ));
    }

    private List<CandidateWinnerResponse> mapWinnerCandidates(ElectionList winnerList) {
        return winnerList.getCandidates()
                .stream()
                .sorted(Comparator.comparing(candidate -> candidate.getPosition().getName()))
                .map(this::toCandidateWinnerResponse)
                .toList();
    }

    private CandidateWinnerResponse toCandidateWinnerResponse(Candidate candidate) {
        return new CandidateWinnerResponse(
                candidate.getPosition().getId(),
                candidate.getPosition().getName(),
                candidate.getId(),
                candidate.getFullName(),
                candidate.getCareer(),
                candidate.getProposal()
        );
    }

    private double calculatePercentage(long value, long total) {
        if (total == 0) {
            return 0.0;
        }

        return Math.round(((double) value * 100.0 / total) * 100.0) / 100.0;
    }
}