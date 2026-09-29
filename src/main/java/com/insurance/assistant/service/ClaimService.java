package com.insurance.assistant.service;

import com.insurance.assistant.dto.CreateClaimRequest;
import com.insurance.assistant.entity.Claim;
import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.repository.ClaimRepository;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyService policyService;

    public ClaimService(ClaimRepository claimRepository, PolicyService policyService) {
        this.claimRepository = claimRepository;
        this.policyService = policyService;
    }

    public Claim getClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
    }

    public List<Claim> getClaimsByPolicyId(Long policyId) {
        return claimRepository.findByPolicyId(policyId);
    }

    public Claim createClaim(CreateClaimRequest request) {
        Policy policy = policyService.getPolicyById(request.getPolicyId());

        Claim claim = new Claim(generateClaimNumber(), policy, request.getClaimType(), request.getDescription(), request.getClaimAmount());
        return claimRepository.save(claim);
    }

    // Simple readable claim numbers like CLM-2026-4821. A real system would
    // likely use a DB sequence, but this is fine for a portfolio project.
    private String generateClaimNumber() {
        int year = Year.now().getValue();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "CLM-" + year + "-" + random;
    }
}
