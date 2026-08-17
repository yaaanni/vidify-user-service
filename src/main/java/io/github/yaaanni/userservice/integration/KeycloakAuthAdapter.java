package io.github.yaaanni.userservice.integration;

import io.github.yaaanni.userservice.exception.KeycloakIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAuthAdapter {

    private final KeycloakAuthClient keycloakAuthClient;

    @Value("${app.keycloak.realm:vidify}")
    private String realm;

    @Value("${app.keycloak.client-id}")
    private String clientId;

    @Value("${app.keycloak.client-secret}")
    private String clientSecret;

    public KeycloakTokenResponse login(String username, String password) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("username", username);
            formData.add("password", password);

            return keycloakAuthClient.getToken(realm, formData);
        } catch (Exception ex) {
            log.error("Failed to login user in Keycloak", ex);
            throw new KeycloakIntegrationException("Invalid credentials or Keycloak error", ex);
        }
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "refresh_token");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("refresh_token", refreshToken);

            return keycloakAuthClient.getToken(realm, formData);
        } catch (Exception ex) {
            log.error("Failed to refresh token in Keycloak", ex);
            throw new KeycloakIntegrationException("Invalid refresh token", ex);
        }
    }

    public void logout(String refreshToken) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("refresh_token", refreshToken);

            keycloakAuthClient.logout(realm, formData);
        } catch (Exception ex) {
            log.warn("Failed to logout in Keycloak, continuing with local logout", ex);
        }
    }
}
