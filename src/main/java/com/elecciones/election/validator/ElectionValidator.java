package com.elecciones.election.validator;

import com.elecciones.common.exception.BusinessException;
import com.elecciones.election.dto.CreateElectionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ElectionValidator {

    public void validateCreate(CreateElectionRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new BusinessException(
                    "La fecha de cierre debe ser posterior a la fecha de inicio",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!request.startDate().isAfter(LocalDateTime.now())) {
            throw new BusinessException(
                    "La fecha de inicio debe ser futura",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}