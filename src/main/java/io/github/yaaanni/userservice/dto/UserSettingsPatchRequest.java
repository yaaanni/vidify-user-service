package io.github.yaaanni.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserSettingsPatchRequest(
        Boolean autoConvertToVideo,

        @Pattern(regexp = "^(EN|RU|ES)$", message = "Language must be EN, RU or ES")
        String language,

        @Email(message = "Invalid email format")
        String notificationEmail
) {
}