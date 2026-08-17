package io.github.yaaanni.userservice.service;

import io.github.yaaanni.userservice.dto.OtpType;

public interface EmailSenderService {
    void sendOtp(String toEmail, String otpCode, OtpType type) throws Exception;
}
