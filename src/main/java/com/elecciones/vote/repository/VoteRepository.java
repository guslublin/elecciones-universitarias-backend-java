package com.elecciones.vote.repository;

import com.elecciones.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    boolean existsByVoterHashAndElectionId(String voterHash, UUID electionId);

    long countByElectionId(UUID electionId);

    long countByElectionListId(UUID electionListId);
}