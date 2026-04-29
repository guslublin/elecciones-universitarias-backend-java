package com.elecciones.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Set<String> roles,
        LocalDateTime createdAt
) {
}