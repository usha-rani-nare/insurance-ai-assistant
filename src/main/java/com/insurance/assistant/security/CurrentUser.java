package com.insurance.assistant.security;

import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Gives controllers the id of whoever is logged in, taken from the JWT
// (the filter stores the email as the authentication name).
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}