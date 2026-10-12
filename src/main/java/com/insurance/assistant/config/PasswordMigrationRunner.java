package com.insurance.assistant.config;

import com.insurance.assistant.entity.User;
import com.insurance.assistant.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Dev/demo only: data.sql inserts the sample users with plain-text passwords,
// so on startup we hash any password that isn't already a BCrypt hash.
// A real project would ship hashed values in a migration instead.
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            String stored = user.getPassword();
            // BCrypt hashes always start with $2a$, $2b$ or $2y$
            if (stored != null && !stored.startsWith("$2")) {
                user.setPassword(passwordEncoder.encode(stored));
                userRepository.save(user);
            }
        }
    }
}