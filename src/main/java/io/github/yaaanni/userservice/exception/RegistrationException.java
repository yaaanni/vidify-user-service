package io.github.yaaanni.userservice.exception;

public class RegistrationException extends RuntimeException {
    public RegistrationException(String message, Exception ex) {
        super(message);
    }
}
