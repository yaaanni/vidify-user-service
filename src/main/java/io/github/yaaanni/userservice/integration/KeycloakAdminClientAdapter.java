package io.github.yaaanni.userservice.integration;

import io.github.yaaanni.userservice.exception.KeycloakIntegrationException;
import io.github.yaaanni.userservice.exception.KeycloakUserConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClientAdapter {

    private final KeycloakAdminClient keycloakAdminClient;

    @Value("${app.keycloak.realm:vidify}")
    private String realm;

    public UUID createUser(KeycloakUserCreateRequest keycloakUser) {
        try {
            ResponseEntity<Void> response = keycloakAdminClient.createUser(realm, keycloakUser);

            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new KeycloakIntegrationException("Keycloak response missing Location header");
            }
            return extractUserIdFromLocation(location);

        } catch (HttpClientErrorException.Conflict ex) {
            throw new KeycloakUserConflictException("Conflict in Keycloak", ex);
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to create user in Keycloak", ex);
        }
    }

    public void deleteUserByEmail(String email) {
        try {
            List<KeycloakUserResponse> users = keycloakAdminClient.searchByEmail(realm, email, true);
            if (!users.isEmpty()) {
                String userId = users.get(0).id();
                keycloakAdminClient.deleteUser(realm, userId);
                log.info("Orphaned user {} deleted from Keycloak", email);
            }
        } catch (Exception ex) {
            log.error("Failed to delete user by email: {}", email, ex);
            throw new KeycloakIntegrationException("Failed to clean up Keycloak user by email", ex);
        }
    }

    public void deleteUserQuietly(UUID userId) {
        try {
            keycloakAdminClient.deleteUser(realm, userId.toString());
            log.info("Compensation successful: user {} deleted from Keycloak", userId);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to execute Keycloak compensation for ID {}. Manual cleanup required!", userId, ex);
        }
    }

    public void updatePassword(UUID userId, String newPassword) {
        try {
            CredentialRepresentation credential = new CredentialRepresentation("password", newPassword, false);
            keycloakAdminClient.resetPassword(realm, userId.toString(), credential);
            log.info("Password successfully updated in Keycloak for user ID: {}", userId);
        } catch (Exception ex) {
            log.error("Failed to update password in Keycloak for user ID: {}", userId, ex);
            throw new KeycloakIntegrationException("Failed to update password in Keycloak", ex);
        }
    }

    private UUID extractUserIdFromLocation(URI location) {
        String path = location.getPath();
        String rawId = path.substring(path.lastIndexOf('/') + 1);
        return UUID.fromString(rawId);
    }
}
