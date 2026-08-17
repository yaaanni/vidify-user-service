package io.github.yaaanni.userservice.controller;

import io.github.yaaanni.userservice.dto.*;
import io.github.yaaanni.userservice.integration.KeycloakTokenResponse;
import io.github.yaaanni.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/init")
    public ResponseEntity<Void> initiateRegistration(@Valid @RequestBody RegistrationInitRequest request) {
        authService.initiateRegistration(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<KeycloakTokenResponse> confirmRegistration(@Valid @RequestBody RegistrationConfirmRequest request) {
        authService.confirmRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/reset/init")
    public ResponseEntity<Void> initiatePasswordReset(@Valid @RequestBody PasswordResetInitRequest request) {
        authService.initiatePasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<KeycloakTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        KeycloakTokenResponse response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<KeycloakTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        KeycloakTokenResponse response = authService.refreshTokens(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(jwt.getSubject(), request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}