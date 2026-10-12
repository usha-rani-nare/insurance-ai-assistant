package com.insurance.assistant.controller;

import com.insurance.assistant.dto.LoginRequest;
import com.insurance.assistant.dto.LoginResponse;
import com.insurance.assistant.entity.User;
import com.insurance.assistant.repository.UserRepository;
import com.insurance.assistant.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(request.email());

        // Same message for "no such email" and "wrong password" so the API
        // doesn't reveal which emails are registered.
        if (found.isEmpty() || !passwordEncoder.matches(request.password(), found.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        User user = found.get();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getName(), user.getEmail()));
    }
}