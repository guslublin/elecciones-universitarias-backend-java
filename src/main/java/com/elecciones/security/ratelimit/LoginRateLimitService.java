package com.elecciones.security.ratelimit;

import com.elecciones.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginRateLimitService {

    private static final String LOGIN_RATE_LIMIT_PREFIX = "rate-limit:login:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public void validateLoginAttempt(String ipAddress) {
        String key = LOGIN_RATE_LIMIT_PREFIX + ipAddress;

        try {
            Long attempts = redisTemplate.opsForValue().increment(key);

            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, WINDOW);
            }

            if (attempts != null && attempts > MAX_ATTEMPTS) {
                throw new BusinessException(
                        "Demasiados intentos de login. Intente nuevamente en 1 minuto.",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }

        } catch (RedisConnectionFailureException ex) {
            // Falla abierto: si Redis no está disponible, se permite el login.
            // Esta decisión prioriza disponibilidad del sistema ante una caída temporal de Redis.
        }
    }
}