package io.github.yaaanni.userservice.service;

import io.github.yaaanni.userservice.dto.OtpType;
import io.github.yaaanni.userservice.exception.InvalidOtpException;
import io.github.yaaanni.userservice.exception.OtpDeliveryException;
import io.github.yaaanni.userservice.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final EmailSenderService emailSenderService;

    private static final String REG_OTP_PREFIX = "otp:reg:";
    private static final String REG_COOLDOWN_PREFIX = "otp:reg:cooldown:";

    private static final String RESET_OTP_PREFIX = "otp:reset:";
    private static final String RESET_COOLDOWN_PREFIX = "otp:reset:cooldown:";

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration COOLDOWN_MINUTES = Duration.ofMinutes(3);

    public void generateAndSaveOtp(String email, String nickname) {
        String cooldownKey = REG_COOLDOWN_PREFIX + email;
        String otpKey = REG_OTP_PREFIX + email;

        checkCooldown(cooldownKey);

        String otp = generateRandomOtp();

        Map<String, String> data = Map.of(
                "code", otp,
                "nickname", nickname
        );

        redisTemplate.opsForHash().putAll(otpKey, data);

        redisTemplate.expire(otpKey, OTP_TTL);

        try {
            emailSenderService.sendOtp(email, otp, OtpType.REGISTRATION);

            redisTemplate.opsForValue().set(cooldownKey, "BLOCKED", COOLDOWN_MINUTES);

            log.info("OTP sent. User {} is blocked for 3 minutes", email);
        } catch (Exception e) {
            log.error("Kafka failure. Deleting OTP so user can retry immediately");
            redisTemplate.delete(otpKey);
            throw new OtpDeliveryException("Failed to send code", e);
        }
    }

    public String verifyAndGetNickname(String email, String otpCode) {
        String key = REG_OTP_PREFIX + email;

        String savedOtp = (String) redisTemplate.opsForHash().get(key, "code");
        String nickname = (String) redisTemplate.opsForHash().get(key, "nickname");

        if (savedOtp == null || !savedOtp.equals(otpCode)) {
            throw new InvalidOtpException("Invalid or expired verification code.");
        }

        redisTemplate.delete(key);

        return nickname;
    }

    public void verifyPasswordResetOtp(String email, String otpCode) {
        String key = RESET_OTP_PREFIX + email;

        String savedOtp = redisTemplate.opsForValue().get(key);

        if (savedOtp == null || !savedOtp.equals(otpCode)) {
            throw new InvalidOtpException("Invalid or expired verification code.");
        }

        redisTemplate.delete(key);
    }

    public void generateAndSavePasswordResetOtp(String email) {
        String cooldownKey = RESET_COOLDOWN_PREFIX + email;
        String otpKey = RESET_OTP_PREFIX + email;

        checkCooldown(cooldownKey);

        String otp = generateRandomOtp();
        redisTemplate.opsForValue().set(otpKey, otp, OTP_TTL);

        try {
            emailSenderService.sendOtp(email, otp, OtpType.PASSWORD_RESET);
            redisTemplate.opsForValue().set(cooldownKey, "BLOCKED", COOLDOWN_MINUTES);
            log.info("Password reset OTP sent. User {} blocked for 3 minutes", email);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP. Cleaning up Redis key: {}", otpKey, e);
            redisTemplate.delete(otpKey);
            throw new OtpDeliveryException("Failed to send code", e);
        }
    }

    private void checkCooldown(String cooldownKey) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new TooManyRequestsException("The email has already been sent. Please wait 3 minutes.");
        }
    }

    private String generateRandomOtp() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
