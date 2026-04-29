package com.elecciones.auth.service;

import com.elecciones.auth.dto.*;
import com.elecciones.common.enums.RoleName;
import com.elecciones.common.exception.BusinessException;
import com.elecciones.security.JwtProperties;
import com.elecciones.security.JwtService;
import com.elecciones.security.token.TokenBlacklistService;
import com.elecciones.user.entity.User;
import com.elecciones.user.entity.UserRole;
import com.elecciones.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Ya existe un usuario registrado con ese email", HttpStatus.CONFLICT);
        }

        Set<RoleName> requestedRoles = request.roles() == null || request.roles().isEmpty()
                ? Set.of(RoleName.VOTER)
                : request.roles();

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .build();

        List<UserRole> roles = requestedRoles.stream()
                .map(roleName -> UserRole.builder()
                        .user(user)
                        .roleName(roleName)
                        .build())
                .toList();

        user.getRoles().addAll(roles);

        User saved = userRepository.save(user);

        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getRoleName().name())
                .toList();

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                Set.copyOf(roles),
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessTokenMinutes() * 60
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException("El token enviado no es un refresh token", HttpStatus.UNAUTHORIZED);
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new BusinessException("El refresh token fue invalidado por logout", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getRoleName().name())
                .toList();

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                Set.copyOf(roles),
                newAccessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessTokenMinutes() * 60
        );
    }

    public void logout(LogoutRequest request) {
        tokenBlacklistService.blacklistRefreshToken(request.refreshToken());
    }

    private UserResponse toUserResponse(User user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                roles,
                user.getCreatedAt()
        );
    }
}