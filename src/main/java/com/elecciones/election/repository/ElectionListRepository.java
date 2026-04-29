package com.elecciones.election.repository;

import com.elecciones.election.entity.ElectionList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElectionListRepository extends JpaRepository<ElectionList, UUID> {

    boolean existsByElectionIdAndNameIgnoreCase(UUID electionId, String name);

    long countByElectionId(UUID electionId);

    Optional<ElectionList> findByIdAndElectionId(UUID id, UUID electionId);

    @EntityGraph(attributePaths = {"candidates", "candidates.position"})
    List<ElectionList> findByElectionId(UUID electionId);
}