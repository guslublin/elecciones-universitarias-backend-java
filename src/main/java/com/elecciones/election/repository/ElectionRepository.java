package com.elecciones.election.repository;

import com.elecciones.election.entity.Election;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ElectionRepository extends JpaRepository<Election, UUID> {

    @EntityGraph(attributePaths = {"positions", "lists", "lists.candidates", "lists.candidates.position"})
    Optional<Election> findWithPositionsAndListsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Election e where e.id = :id")
    Optional<Election> findByIdForUpdate(UUID id);
}