package io.github.yaaanni.userservice.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUserResponse(
        String id,
        String username,
        String email,
        boolean enabled,
        boolean emailVerified
) {
}
