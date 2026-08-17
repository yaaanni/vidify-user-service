package io.github.yaaanni.userservice.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String message,
        int status,
        String error,
        OffsetDateTime timestamp
) {
    public ErrorResponse(String message, int status, String error) {
        this(message, status, error, OffsetDateTime.now());
    }
}
