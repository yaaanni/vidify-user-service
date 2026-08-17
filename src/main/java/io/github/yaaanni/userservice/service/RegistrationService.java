package io.github.yaaanni.userservice.service;

import io.github.yaaanni.userservice.dto.PasswordResetConfirmRequest;
import io.github.yaaanni.userservice.dto.PasswordResetInitRequest;
import io.github.yaaanni.userservice.dto.RegistrationConfirmRequest;
import io.github.yaaanni.userservice.dto.RegistrationInitRequest;
import io.github.yaaanni.userservice.entity.User;
import io.github.yaaanni.userservice.exception.*;
import io.github.yaaanni.userservice.integration.CredentialRepresentation;
import io.github.yaaanni.userservice.integration.KeycloakAdminClientAdapter;
import io.github.yaaanni.userservice.integration.KeycloakUserCreateRequest;
import io.github.yaaanni.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final KeycloakAdminClientAdapter keycloakAdapter;
    private final UserPersistenceService userPersistenceService;

    @Value("${KEYCLOAK_REALM:vidify}")
    private String realm;

    public void initiateRegistration(RegistrationInitRequest request) {
        checkLocalDatabase(request.email(), request.nickname());

        otpService.generateAndSaveOtp(request.email(), request.nickname());
    }

    public void confirmRegistration(RegistrationConfirmRequest request) {
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
}
