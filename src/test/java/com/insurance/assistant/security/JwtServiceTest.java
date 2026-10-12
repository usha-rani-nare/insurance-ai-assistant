package com.insurance.assistant.security;

import com.insurance.assistant.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-at-least-32-chars";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private User testUser() {
        User user = new User("Usha", "usha@example.com", "not-a-real-hash");
        user.setId(1L);
        return user;
    }

    @Test
    void generatedToken_isValid_andContainsTheUsersEmail() {
        JwtService jwtService = new JwtService(SECRET, ONE_HOUR_MS);

        String token = jwtService.generateToken(testUser());

        assertTrue(jwtService.isValid(token));
        assertEquals("usha@example.com", jwtService.extractEmail(token));
    }

    @Test
    void tokenSignedWithADifferentSecret_isRejected() {
        JwtService attacker = new JwtService("a-completely-different-secret-key-also-32-chars-long", ONE_HOUR_MS);
        JwtService server = new JwtService(SECRET, ONE_HOUR_MS);

        String forgedToken = attacker.generateToken(testUser());

        assertFalse(server.isValid(forgedToken));
    }

    @Test
    void expiredToken_isRejected() {
        // A negative lifetime means the token is already expired when created.
        JwtService jwtService = new JwtService(SECRET, -1000L);

        String token = jwtService.generateToken(testUser());

        assertFalse(jwtService.isValid(token));
    }

    @Test
    void garbageInput_isRejected() {
        JwtService jwtService = new JwtService(SECRET, ONE_HOUR_MS);

        assertFalse(jwtService.isValid("not-a-token"));
        assertFalse(jwtService.isValid(""));
    }
}