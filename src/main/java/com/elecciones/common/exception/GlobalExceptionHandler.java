package com.elecciones.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ProblemDetail build(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // ✅ negocio (409, 404, etc)
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getStatus(), "Error de negocio", ex.getMessage(), request);
    }

    // ✅ 401 credenciales
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "No autenticado", "Credenciales inválidas", request);
    }

    // ✅ 403
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleForbidden(HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Acceso denegado", "No tiene permisos para este recurso", request);
    }

    // ✅ validaciones
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "Solicitud inválida", "Error de validación", request);
        problem.setProperty("errors", errors);

        return problem;
    }

    // ✅ 404 genérico
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ProblemDetail handleNotFound(HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No encontrado", "Entidad no encontrada", request);
    }

    // ✅ rate limit
    @ExceptionHandler(RateLimitException.class)
    public ProblemDetail handleRateLimit(RateLimitException ex, HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Rate limit", ex.getMessage(), request);
    }

    // ✅ fallback
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Error interno del servidor", request);
    }
}