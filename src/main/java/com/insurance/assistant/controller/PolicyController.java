package com.insurance.assistant.controller;

import com.insurance.assistant.dto.PolicyResponse;
import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.service.PolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/{id}")
    public PolicyResponse getPolicy(@PathVariable Long id) {
        Policy policy = policyService.getPolicyById(id);
        return new PolicyResponse(policy);
    }

    // Stands in for "my policies" until real authentication exists (a later
    // phase) - the frontend passes the demo user id explicitly for now.
    @GetMapping("/user/{userId}")
    public List<PolicyResponse> getPoliciesForUser(@PathVariable Long userId) {
        return policyService.getPoliciesByUserId(userId).stream()
                .map(PolicyResponse::new)
                .toList();
    }
}
