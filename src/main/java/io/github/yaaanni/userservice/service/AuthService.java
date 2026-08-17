package io.github.yaaanni.userservice.service;

import io.github.yaaanni.userservice.dto.PasswordResetConfirmRequest;
import io.github.yaaanni.userservice.dto.PasswordResetInitRequest;
import io.github.yaaanni.userservice.dto.RegistrationConfirmRequest;
import io.github.yaaanni.userservice.dto.RegistrationInitRequest;
import io.github.yaaanni.userservice.entity.User;
import io.github.yaaanni.userservice.exception.*;
import io.github.yaaanni.userservice.integration.*;
import io.github.yaaanni.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final KeycloakAdminClientAdapter keycloakAdapter;
    private final UserPersistenceService userPersistenceService;
    private final KeycloakAuthAdapter authAdapter;
    private final JwtDecoder jwtDecoder;

    @Value("${KEYCLOAK_REALM:vidify}")
    private String realm;

    private final StringRedisTemplate redisTemplate;

    private static final String ACTIVE_SESSION_PREFIX = "active_session:";

    public void initiateRegistration(RegistrationInitRequest request) {
        checkLocalDatabase(request.email(), request.nickname());

        otpService.generateAndSaveOtp(request.email(), request.nickname());
    }

    public KeycloakTokenResponse confirmRegistration(RegistrationConfirmRequest request) {
        validatePasswordsMatch(request.password(), request.passwordConfirm());

        String nickname = otpService.verifyAndGetNickname(request.email(), request.otpCode());

        checkLocalDatabase(request.email(), nickname);

        KeycloakUserCreateRequest keycloakRequest = new KeycloakUserCreateRequest(
                nickname,
                request.email(),
                true,
                true,
                List.of(new CredentialRepresentation("password", request.password(), false))
        );

        UUID keycloakId = createKeycloakUserWithHealing(keycloakRequest);

        try {
            userPersistenceService.saveUser(keycloakId, request.email(), nickname);
        } catch (Exception ex) {
            log.error("Database save failed. Rolling back Keycloak ID: {}", keycloakId, ex);
            keycloakAdapter.deleteUserQuietly(keycloakId);
            throw new RegistrationException("Failed to complete registration", ex);
        }

        return login(request.email(), request.password());
    }

    public void initiatePasswordReset(PasswordResetInitRequest request) {
        if (!userRepository.existsByEmail(request.email())) {
            log.info("Password reset requested for non-existent email: {}", request.email());
            return;
        }

        otpService.generateAndSavePasswordResetOtp(request.email());
    }

    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        validatePasswordsMatch(request.newPassword(), request.newPasswordConfirm());

        otpService.verifyPasswordResetOtp(request.email(), request.otpCode());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(String.format("User with email '%s' not found", request.email())));

        keycloakAdapter.updatePassword(user.getId(), request.newPassword());

        invalidateSession(user.getId().toString());
    }

    public void saveActiveSession(String userId, String identifier, Duration ttl) {
        String key = ACTIVE_SESSION_PREFIX + userId;
        redisTemplate.opsForValue().set(key, identifier, ttl);
    }

    public String getActiveSession(String userId) {
        return redisTemplate.opsForValue().get(ACTIVE_SESSION_PREFIX + userId);
    }

    public void invalidateSession(String userId) {
        redisTemplate.delete(ACTIVE_SESSION_PREFIX + userId);
    }

    public KeycloakTokenResponse login(String email, String password) {
        KeycloakTokenResponse tokenResponse = authAdapter.login(email, password);

        processAndSaveActiveSession(tokenResponse);

        return tokenResponse;
    }

    public KeycloakTokenResponse refreshTokens(String refreshToken) {
        KeycloakTokenResponse tokenResponse = authAdapter.refreshToken(refreshToken);

        processAndSaveActiveSession(tokenResponse);

        return tokenResponse;
    }

    public void logout(String userId, String refreshToken) {
        invalidateSession(userId);

        authAdapter.logout(refreshToken);
    }

    private void validatePasswordsMatch(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new PasswordMismatchException("Passwords do not match");
        }
    }

    private void checkLocalDatabase(String email, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    String.format("User with email '%s' already exists", email)
            );
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new UserAlreadyExistsException(
                    String.format("User with nickname '%s' already exists", nickname)
            );
        }
    }

    private UUID createKeycloakUserWithHealing(KeycloakUserCreateRequest keycloakRequest) {
        try {
            return keycloakAdapter.createUser(keycloakRequest);
        } catch (KeycloakUserConflictException ex) {
            log.warn("Orphaned Keycloak user detected for {}. Initiating self-healing...", keycloakRequest.email());

            keycloakAdapter.deleteUserByEmail(keycloakRequest.email());

            return keycloakAdapter.createUser(keycloakRequest);
        }
    }

    private void processAndSaveActiveSession(KeycloakTokenResponse tokenResponse) {
        Jwt accessJwt = jwtDecoder.decode(tokenResponse.accessToken());

        String userId = accessJwt.getSubject();

        String identifier = accessJwt.getClaimAsString("sid") != null
                ? accessJwt.getClaimAsString("sid")
                : accessJwt.getId();

        long ttlSeconds = tokenResponse.refreshExpiresIn();

        saveActiveSession(userId, identifier, Duration.ofSeconds(ttlSeconds));
    }
}
