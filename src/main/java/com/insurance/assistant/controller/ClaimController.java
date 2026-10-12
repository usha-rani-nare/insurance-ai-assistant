package com.insurance.assistant.controller;

import com.insurance.assistant.dto.ClaimResponse;
import com.insurance.assistant.dto.CreateClaimRequest;
import com.insurance.assistant.entity.Claim;
import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.security.CurrentUser;
import com.insurance.assistant.service.ClaimService;
import com.insurance.assistant.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final PolicyService policyService;
    private final CurrentUser currentUser;

    public ClaimController(ClaimService claimService, PolicyService policyService, CurrentUser currentUser) {
        this.claimService = claimService;
        this.policyService = policyService;
        this.currentUser = currentUser;
    }

    @GetMapping("/{id}")
    public ClaimResponse getClaim(@PathVariable Long id) {
        // Search only the claims on the logged-in user's own policies.
        for (Policy policy : policyService.getPoliciesByUserId(currentUser.getId())) {
            for (Claim claim : claimService.getClaimsByPolicyId(policy.getId())) {
                if (claim.getId().equals(id)) {
                    return new ClaimResponse(claim);
                }
            }
        }
        throw new ResourceNotFoundException("Claim not found with id: " + id);
    }

    @GetMapping("/policy/{policyId}")
    public List<ClaimResponse> getClaimsForPolicy(@PathVariable Long policyId) {
        boolean ownsPolicy = policyService.getPoliciesByUserId(currentUser.getId()).stream()
                .anyMatch(p -> p.getId().equals(policyId));
        if (!ownsPolicy) {
            throw new ResourceNotFoundException("Policy not found with id: " + policyId);
        }
        return claimService.getClaimsByPolicyId(policyId).stream()
                .map(ClaimResponse::new)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        // The policy in the request body must belong to the logged-in user.
        // Someone else's policy id gets the same "not found" as a missing one.
        boolean ownsPolicy = policyService.getPoliciesByUserId(currentUser.getId()).stream()
                .anyMatch(p -> p.getId().equals(request.getPolicyId()));
        if (!ownsPolicy) {
            throw new ResourceNotFoundException("Policy not found with id: " + request.getPolicyId());
        }

        Claim claim = claimService.createClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ClaimResponse(claim));
    }
}