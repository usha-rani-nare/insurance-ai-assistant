package com.insurance.assistant.service;

import com.insurance.assistant.dto.RegisterUserRequest;
import com.insurance.assistant.entity.User;
import com.insurance.assistant.exception.DuplicateEmailException;
import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        // Plain text for now - the security phase replaces this with BCrypt hashing.
        User user = new User(request.getName(), request.getEmail(), request.getPassword(), request.getPhone());
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
