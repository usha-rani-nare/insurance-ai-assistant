package com.insurance.assistant.service;

import com.insurance.assistant.dto.RegisterUserRequest;
import com.insurance.assistant.entity.User;
import com.insurance.assistant.exception.DuplicateEmailException;
import com.insurance.assistant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServicePasswordTest {

    @Mock
    private UserRepository userRepository;

    // A real encoder, so the test checks real BCrypt behaviour.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    private RegisterUserRequest request(String email, String password) {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setName("Test User");
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void register_storesABcryptHash_notThePlainPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(request("new@example.com", "secret123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        String stored = saved.getValue().getPassword();

        assertNotEquals("secret123", stored);
        assertTrue(stored.startsWith("$2"));
        assertTrue(passwordEncoder.matches("secret123", stored));
    }

    @Test
    void register_rejectsDuplicateEmail_andSavesNothing() {
        when(userRepository.existsByEmail("usha@example.com")).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> userService.register(request("usha@example.com", "secret123")));

        assertEquals("An account with this email already exists", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}