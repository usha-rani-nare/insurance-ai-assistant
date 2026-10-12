package com.insurance.assistant.controller;

import com.insurance.assistant.config.PasswordConfig;
import com.insurance.assistant.config.SecurityConfig;
import com.insurance.assistant.entity.User;
import com.insurance.assistant.repository.UserRepository;
import com.insurance.assistant.security.CurrentUser;
import com.insurance.assistant.security.JwtAuthFilter;
import com.insurance.assistant.security.JwtService;
import com.insurance.assistant.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web-layer test: real security filter chain, real JWT and BCrypt code,
// but repositories and services are mocked so no database is needed.
@WebMvcTest({AuthController.class, PolicyController.class})
@Import({SecurityConfig.class, PasswordConfig.class, JwtAuthFilter.class, JwtService.class, CurrentUser.class})
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-web-tests-at-least-32-characters",
        "app.jwt.expiration-ms=3600000"
})
class SecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private PolicyService policyService;

    private User usha;
    private User newUser;

    @BeforeEach
    void setUp() {
        usha = new User("Usha", "usha@example.com", passwordEncoder.encode("password123"));
        usha.setId(1L);
        newUser = new User("New User", "newuser@example.com", passwordEncoder.encode("secret123"));
        newUser.setId(4L);

        when(userRepository.findByEmail("usha@example.com")).thenReturn(Optional.of(usha));
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.of(newUser));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    // ---- Login ----

    @Test
    void login_withCorrectPassword_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usha@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usha@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_withUnknownEmail_returns401_withTheSameMessage() throws Exception {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ---- Protected endpoints ----

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/policies/user/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/policies/user/1")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_returnsOwnData() throws Exception {
        when(policyService.getPoliciesByUserId(4L)).thenReturn(List.of());

        mockMvc.perform(get("/api/policies/user/4")
                        .header("Authorization", bearer(newUser)))
                .andExpect(status().isOk());
    }

    // ---- Ownership (IDOR) ----

    @Test
    void readingAnotherUsersPolicies_returns403() throws Exception {
        mockMvc.perform(get("/api/policies/user/1")
                        .header("Authorization", bearer(newUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only view your own policies"));
    }

    @Test
    void readingAnotherUsersPolicyById_returns404() throws Exception {
        // The logged-in user (4) owns no policies, so policy 1 can't be found.
        when(policyService.getPoliciesByUserId(4L)).thenReturn(List.of());

        mockMvc.perform(get("/api/policies/1")
                        .header("Authorization", bearer(newUser)))
                .andExpect(status().isNotFound());
    }
}