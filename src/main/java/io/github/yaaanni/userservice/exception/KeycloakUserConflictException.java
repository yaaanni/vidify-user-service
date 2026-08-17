package io.github.yaaanni.userservice.exception;

import org.springframework.web.client.HttpClientErrorException;

public class KeycloakUserConflictException extends RuntimeException {
    public KeycloakUserConflictException(String message, HttpClientErrorException.Conflict ex) {
        super(message);
    }
}
