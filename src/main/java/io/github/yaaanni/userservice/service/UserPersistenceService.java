package io.github.yaaanni.userservice.service;

import io.github.yaaanni.userservice.entity.User;
import io.github.yaaanni.userservice.entity.UserSettings;
import io.github.yaaanni.userservice.entity.UserStatus;
import io.github.yaaanni.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPersistenceService {
    private final UserRepository userRepository;

    @Transactional
    public void saveUser(UUID keycloakId, String email, String nickname) {
        UserSettings settings = UserSettings.builder()
                .notificationEmail(email)
                .build();

        User user = User.builder()
                .id(keycloakId)
                .email(email)
                .nickname(nickname)
                .status(UserStatus.ACTIVE)
                .build();

        user.setUserSettings(settings);
        userRepository.save(user);
    }
}
