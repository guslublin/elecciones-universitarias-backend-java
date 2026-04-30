package com.elecciones.audit.service;

import com.elecciones.audit.dto.AuditLogResponse;
import com.elecciones.audit.entity.AuditLog;
import com.elecciones.audit.repository.AuditLogRepository;
import com.elecciones.common.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            UUID actorId,
            String actor,
            AuditAction action,
            String entityType,
            UUID entityId,
            Map<String, Object> detail
    ) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .detail(detail == null ? Map.of() : detail)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return auditLogRepository.findAll(pageRequest)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorId(),
                auditLog.getActor(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDetail(),
                auditLog.getCreatedAt()
        );
    }
}