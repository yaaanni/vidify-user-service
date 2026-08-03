package io.github.yaaanni.userservice.service;

import io.github.yaaanni.userservice.dto.UserSettingsResponse;
import io.github.yaaanni.userservice.entity.UserSettings;
import io.github.yaaanni.userservice.mapper.UserSettingsMapper;
import io.github.yaaanni.userservice.repository.UserSettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository repository;
    private final UserSettingsMapper mapper;

    @Transactional
    public UserSettingsResponse getOrCreateSettings(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        UserSettings settings = repository.findById(userId)
                .orElseGet(() -> createDefaultSettings(userId, jwt));

        return mapper.toResponse(settings);
    }

    private UserSettings createDefaultSettings(UUID userId, Jwt jwt) {
        String jwtEmail = Optional.ofNullable(jwt.getClaimAsString("email"))
                .filter(email -> !email.isBlank())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Required 'email' claim is missing in JWT token"
                ));

        UserSettings newSettings = UserSettings.builder()
                .userId(userId)
                .language("EN")
                .autoConvertToVideo(false)
                .notificationEmail(jwtEmail)
                .build();

        return repository.save(newSettings);
    }
}
