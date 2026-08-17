package io.github.yaaanni.userservice.integration;

import java.util.List;

public record KeycloakUserCreateRequest(
        String username,
        String email,
        boolean enabled,
        boolean emailVerified,
        List<CredentialRepresentation> credentials
) {
}
