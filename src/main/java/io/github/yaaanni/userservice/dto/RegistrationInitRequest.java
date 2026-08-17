package io.github.yaaanni.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationInitRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Nickname is required")
        @Size(min = 3, max = 20, message = "Nickname must be between 3 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Nickname can only contain letters, numbers, underscores, dots, and hyphens")
        String nickname
) {
}