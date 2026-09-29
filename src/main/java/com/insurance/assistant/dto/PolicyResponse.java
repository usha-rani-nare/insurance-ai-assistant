package com.insurance.assistant.dto;

import com.insurance.assistant.entity.Policy;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PolicyResponse {

    private Long id;
    private String policyNumber;
    private String planName;
    private BigDecimal coverageAmount;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public PolicyResponse(Policy policy) {
        this.id = policy.getId();
        this.policyNumber = policy.getPolicyNumber();
        this.planName = policy.getInsurancePlan().getPlanName();
        this.coverageAmount = policy.getInsurancePlan().getCoverageAmount();
        this.premiumAmount = policy.getInsurancePlan().getPremiumAmount();
        this.startDate = policy.getStartDate();
        this.endDate = policy.getEndDate();
        this.status = policy.getStatus().name();
    }

    public Long getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getPlanName() {
        return planName;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public BigDecimal getPremiumAmount() {
        return premiumAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }
}
