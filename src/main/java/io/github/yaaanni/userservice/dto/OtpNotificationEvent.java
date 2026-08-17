package io.github.yaaanni.userservice.dto;

public record OtpNotificationEvent(
        String email,
        String otpCode,
        OtpType type
) {
}
