package com.elecciones.election.repository;

import com.elecciones.election.entity.Election;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ElectionRepository extends JpaRepository<Election, UUID> {

    @EntityGraph(attributePaths = {"positions", "lists", "lists.candidates", "lists.candidates.position"})
    Optional<Election> findWithPositionsAndListsById(UUID id);
}