package com.elecciones.audit.repository;

import com.elecciones.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, UUID entityId);

    @Query(value = """
            select *
            from audit_logs
            where
                (entity_type = 'ELECTION' and entity_id = cast(:electionId as uuid))
                or detail ->> 'electionId' = cast(:electionId as text)
            order by created_at asc
            """, nativeQuery = true)
    List<AuditLog> findElectionAuditEvents(UUID electionId);
}