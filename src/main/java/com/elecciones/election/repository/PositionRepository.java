package com.elecciones.election.repository;

import com.elecciones.election.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {

    boolean existsByElectionIdAndNameIgnoreCase(UUID electionId, String name);

    List<Position> findByElectionId(UUID electionId);

    long countByElectionId(UUID electionId);
}