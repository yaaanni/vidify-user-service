package io.github.yaaanni.userservice.integration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.*;

import java.util.List;

@HttpExchange("/admin/realms/{realm}")
public interface KeycloakAdminClient {

    @PostExchange("/users")
    ResponseEntity<Void> createUser(
            @PathVariable String realm,
            @RequestBody KeycloakUserCreateRequest user
    );

    @GetExchange("/users/{userId}")
    KeycloakUserResponse getUser(
            @PathVariable String realm,
            @PathVariable String userId
    );

    @GetExchange("/users")
    List<KeycloakUserResponse> searchByEmail(
            @PathVariable String realm,
            @RequestParam("email") String email,
            @RequestParam("exact") boolean exact
    );

    @DeleteExchange("/users/{userId}")
    ResponseEntity<Void> deleteUser(
            @PathVariable String realm,
            @PathVariable String userId
    );

    @PutExchange("/users/{userId}/reset-password")
    void resetPassword(
            @PathVariable String realm,
            @PathVariable String userId,
            @RequestBody CredentialRepresentation credential
    );
}
