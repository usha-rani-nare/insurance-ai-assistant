package com.insurance.assistant.controller;

import com.insurance.assistant.dto.RegisterUserRequest;
import com.insurance.assistant.dto.UserResponse;
import com.insurance.assistant.entity.User;
import com.insurance.assistant.security.CurrentUser;
import com.insurance.assistant.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(user));
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        // A logged-in user can only read their own profile.
        if (!id.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own profile");
        }
        User user = userService.getUserById(id);
        return new UserResponse(user);
    }
}