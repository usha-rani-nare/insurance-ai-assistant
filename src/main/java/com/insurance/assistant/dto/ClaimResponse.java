package com.insurance.assistant.dto;

import com.insurance.assistant.entity.Claim;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClaimResponse {

    private Long id;
    private String claimNumber;
    private String policyNumber;
    private String claimType;
    private String description;
    private BigDecimal claimAmount;
    private String status;
    private LocalDateTime submittedAt;

    public ClaimResponse(Claim claim) {
        this.id = claim.getId();
        this.claimNumber = claim.getClaimNumber();
        this.policyNumber = claim.getPolicy().getPolicyNumber();
        this.claimType = claim.getClaimType();
        this.description = claim.getDescription();
        this.claimAmount = claim.getClaimAmount();
        this.status = claim.getStatus().name();
        this.submittedAt = claim.getSubmittedAt();
    }

    public Long getId() {
        return id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getClaimType() {
        return claimType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getClaimAmount() {
        return claimAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
