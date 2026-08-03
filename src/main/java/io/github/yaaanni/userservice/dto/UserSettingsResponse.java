package io.github.yaaanni.userservice.dto;

import java.util.UUID;

public record UserSettingsResponse(
        UUID userId,
        boolean autoConvertToVideo,
        String notificationEmail,
        String language
) {}