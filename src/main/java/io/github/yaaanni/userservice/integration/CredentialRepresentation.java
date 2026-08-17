package io.github.yaaanni.userservice.integration;

public record CredentialRepresentation(
        String type,
        String value,
        boolean temporary
) {
}
