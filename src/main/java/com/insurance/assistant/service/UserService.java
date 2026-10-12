package com.insurance.assistant.service;

import com.insurance.assistant.dto.RegisterUserRequest;
import com.insurance.assistant.entity.User;
import com.insurance.assistant.exception.DuplicateEmailException;
import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        // Only the BCrypt hash is stored, never the plain password.
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), request.getEmail(), hashedPassword, request.getPhone());
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}