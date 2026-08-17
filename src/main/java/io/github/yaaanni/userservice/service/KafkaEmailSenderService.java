package io.github.yaaanni.userservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.yaaanni.userservice.dto.OtpNotificationEvent;
import io.github.yaaanni.userservice.dto.OtpType;
import io.github.yaaanni.userservice.exception.OtpDeliveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class KafkaEmailSenderService implements EmailSenderService {

    private final KafkaTemplate<String, Object> otpKafkaTemplate;

    public KafkaEmailSenderService(@Qualifier("otpKafkaTemplate") KafkaTemplate<String, Object> otpKafkaTemplate) {
        this.otpKafkaTemplate = otpKafkaTemplate;
    }

    @Value("${app.kafka.topics.notification}")
    private String notificationTopic;

    @CircuitBreaker(name = "kafkaOtp", fallbackMethod = "fallbackSendOtp")
    @Override
    public void sendOtp(String toEmail, String otpCode, OtpType type) throws Exception {
        log.info("Sending OTP for email {} to Kafka topic: {}", toEmail, notificationTopic);

        OtpNotificationEvent event = new OtpNotificationEvent(toEmail, otpCode, type);
        otpKafkaTemplate.send(notificationTopic, toEmail, event).get(3, TimeUnit.SECONDS);

        log.info("Sent {} OTP for email {} to Kafka topic: {}", type, event.email(), notificationTopic);
    }

    public void fallbackSendOtp(String toEmail, String otpCode, Throwable t) {
        log.error("Kafka unavailable or Circuit Breaker triggered for email {}. Reason: {}", toEmail, t.getMessage());

        throw new OtpDeliveryException("Service is temporarily unavailable. Please try again later.", t);
    }
}
