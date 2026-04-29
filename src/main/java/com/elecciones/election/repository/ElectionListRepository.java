package com.elecciones.election.repository;

import com.elecciones.election.entity.ElectionList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ElectionListRepository extends JpaRepository<ElectionList, UUID> {

    boolean existsByElectionIdAndNameIgnoreCase(UUID electionId, String name);
}