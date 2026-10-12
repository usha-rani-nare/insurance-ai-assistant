package com.insurance.assistant.controller;

import com.insurance.assistant.dto.PolicyResponse;
import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.security.CurrentUser;
import com.insurance.assistant.service.PolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final CurrentUser currentUser;

    public PolicyController(PolicyService policyService, CurrentUser currentUser) {
        this.policyService = policyService;
        this.currentUser = currentUser;
    }

    @GetMapping("/{id}")
    public PolicyResponse getPolicy(@PathVariable Long id) {
        // Only look inside the logged-in user's own policies. Someone else's
        // policy id gets the same "not found" as one that doesn't exist.
        Policy policy = policyService.getPoliciesByUserId(currentUser.getId()).stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + id));
        return new PolicyResponse(policy);
    }

    @GetMapping("/user/{userId}")
    public List<PolicyResponse> getPoliciesForUser(@PathVariable Long userId) {
        if (!userId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own policies");
        }
        return policyService.getPoliciesByUserId(userId).stream()
                .map(PolicyResponse::new)
                .toList();
    }
}