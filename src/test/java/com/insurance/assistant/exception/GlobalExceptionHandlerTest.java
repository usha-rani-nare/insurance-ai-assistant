package com.insurance.assistant.exception;

import com.insurance.assistant.config.PasswordConfig;
import com.insurance.assistant.config.SecurityConfig;
import com.insurance.assistant.controller.AuthController;
import com.insurance.assistant.controller.PolicyController;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checks that client mistakes come back as proper 4xx errors with a readable
// message, instead of a generic 500.
@WebMvcTest({AuthController.class, PolicyController.class})
@Import({SecurityConfig.class, PasswordConfig.class, JwtAuthFilter.class, JwtService.class, CurrentUser.class})
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-web-tests-at-least-32-characters",
        "app.jwt.expiration-ms=3600000"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private PolicyService policyService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        User user = new User("Test User", "test@example.com", "not-a-real-hash");
        user.setId(4L);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        bearerToken = "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{this is not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void wrongHttpMethod_returns405() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method GET is not supported for this endpoint"));
    }

    @Test
    void wrongContentType_returns415() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/policies/abc")
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));
    }

    @Test
    void unknownUrl_returns404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}