package com.elecciones.audit.service;

import com.elecciones.audit.dto.AuditVerifyRequest;
import com.elecciones.audit.dto.AuditVerifyResponse;
import com.elecciones.security.HmacService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditVerifyService {

    private final HmacService hmacService;

    public AuditVerifyResponse verify(AuditVerifyRequest request) {
        boolean valid = hmacService.verifyPayload(request.payload(), request.signature());

        if (valid) {
            return new AuditVerifyResponse(
                    true,
                    "La firma HMAC es válida. El payload no fue alterado."
            );
        }

        return new AuditVerifyResponse(
                false,
                "La firma HMAC no es válida. El payload pudo haber sido alterado."
        );
    }
}