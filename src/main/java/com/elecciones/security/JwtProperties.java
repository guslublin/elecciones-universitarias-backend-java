package com.elecciones.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenMinutes,
        long refreshTokenDays
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            secret = "ChangeThisJwtSecret_Min32Chars_Test_2026";
        }
        if (accessTokenMinutes <= 0) {
            accessTokenMinutes = 15;
        }
        if (refreshTokenDays <= 0) {
            refreshTokenDays = 7;
        }
    }
}