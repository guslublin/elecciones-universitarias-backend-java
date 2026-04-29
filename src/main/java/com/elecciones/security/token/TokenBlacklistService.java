package com.elecciones.security.token;

import com.elecciones.common.exception.BusinessException;
import com.elecciones.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void blacklistRefreshToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException("El token enviado no es un refresh token", HttpStatus.BAD_REQUEST);
        }

        Instant expiration = jwtService.extractExpiration(refreshToken);
        long secondsRemaining = Duration.between(Instant.now(), expiration).getSeconds();

        if (secondsRemaining <= 0) {
            throw new BusinessException("El refresh token ya está expirado", HttpStatus.UNAUTHORIZED);
        }

        try {
            redisTemplate.opsForValue().set(
                    buildKey(refreshToken),
                    "invalidated",
                    Duration.ofSeconds(secondsRemaining)
            );
        } catch (RedisConnectionFailureException ex) {
            // Fase actual: falla abierto para no romper disponibilidad.
            // Luego se registrará fallback en auditoría.
        }
    }

    public boolean isBlacklisted(String refreshToken) {
        try {
            Boolean exists = redisTemplate.hasKey(buildKey(refreshToken));
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException ex) {
            return false;
        }
    }

    private String buildKey(String refreshToken) {
        return BLACKLIST_PREFIX + refreshToken;
    }
}