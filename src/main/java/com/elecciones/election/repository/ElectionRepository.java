package com.elecciones.election.repository;

import com.elecciones.common.enums.ElectionStatus;
import com.elecciones.election.entity.Election;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElectionRepository extends JpaRepository<Election, UUID> {

    @EntityGraph(attributePaths = {
            "positions",
            "lists",
            "lists.candidates",
            "lists.candidates.position"
    })
    Optional<Election> findWithPositionsAndListsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Election e where e.id = :id")
    Optional<Election> findByIdForUpdate(@Param("id") UUID id);

    List<Election> findByStatusAndEndDateBefore(
            ElectionStatus status,
            LocalDateTime dateTime
    );
}