package com.insurance.assistant.repository;

import com.insurance.assistant.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByPolicyId(Long policyId);

    Optional<Claim> findByClaimNumber(String claimNumber);
}
