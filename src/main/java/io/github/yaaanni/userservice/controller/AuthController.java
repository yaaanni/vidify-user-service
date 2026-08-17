package io.github.yaaanni.userservice.controller;

import io.github.yaaanni.userservice.dto.PasswordResetConfirmRequest;
import io.github.yaaanni.userservice.dto.PasswordResetInitRequest;
import io.github.yaaanni.userservice.dto.RegistrationConfirmRequest;
import io.github.yaaanni.userservice.dto.RegistrationInitRequest;
import io.github.yaaanni.userservice.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;

    @PostMapping("/register/init")
    public ResponseEntity<Void> initiateRegistration(@Valid @RequestBody RegistrationInitRequest request) {
        registrationService.initiateRegistration(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<Void> confirmRegistration(@Valid @RequestBody RegistrationConfirmRequest request) {
        registrationService.confirmRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/reset/init")
    public ResponseEntity<Void> initiatePasswordReset(@Valid @RequestBody PasswordResetInitRequest request) {
        registrationService.initiatePasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        registrationService.confirmPasswordReset(request);
        return ResponseEntity.ok().build();
    }
}
