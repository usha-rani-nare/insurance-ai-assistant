package com.insurance.assistant.controller;

import com.insurance.assistant.dto.ClaimResponse;
import com.insurance.assistant.dto.CreateClaimRequest;
import com.insurance.assistant.entity.Claim;
import com.insurance.assistant.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/{id}")
    public ClaimResponse getClaim(@PathVariable Long id) {
        Claim claim = claimService.getClaimById(id);
        return new ClaimResponse(claim);
    }

    @GetMapping("/policy/{policyId}")
    public List<ClaimResponse> getClaimsForPolicy(@PathVariable Long policyId) {
        return claimService.getClaimsByPolicyId(policyId).stream()
                .map(ClaimResponse::new)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        Claim claim = claimService.createClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ClaimResponse(claim));
    }
}
