package com.insurance.assistant.service;

import com.insurance.assistant.dto.CreateClaimRequest;
import com.insurance.assistant.entity.Claim;
import com.insurance.assistant.entity.InsurancePlan;
import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.security.CurrentUser;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Phase 7: tools the AI model can call during a conversation to look up
 * real data or take an action, instead of only answering from the policy
 * document context (RAG). Each @Tool method is exposed to the model via
 * ChatClient's .tools(...); the model decides on its own when a question
 * needs one of these versus just answering from the document context.
 *
 * Phase 8: every tool only works on the logged-in user's own policies and
 * claims. The user comes from the JWT (CurrentUser), never from the chat
 * text, so the model can't be talked into touching someone else's data.
 */
@Component
public class InsuranceTools {

    private final PolicyService policyService;
    private final ClaimService claimService;
    private final CurrentUser currentUser;

    public InsuranceTools(PolicyService policyService, ClaimService claimService, CurrentUser currentUser) {
        this.policyService = policyService;
        this.claimService = claimService;
        this.currentUser = currentUser;
    }

    @Tool(description = "Get the details of one of the logged-in customer's insurance policies by its " +
            "policy ID number. Use this when the customer asks about their own policy's coverage dates, " +
            "status, or plan, and has given you a policy ID. If they haven't given a " +
            "policy ID, ask them for it first instead of guessing.")
    public String getPolicyDetails(
            @ToolParam(description = "The numeric policy ID, e.g. 1, 2, 3") Long policyId) {
        Optional<Policy> found = findOwnPolicy(policyId);
        if (found.isEmpty()) {
            return "No policy was found with ID " + policyId + ". Double-check the policy ID and try again.";
        }

        Policy policy = found.get();
        InsurancePlan plan = policy.getInsurancePlan();
        return """
                Policy Number: %s
                Plan: %s
                Plan Description: %s
                Coverage Amount: \u20b9%s
                Premium Amount: \u20b9%s
                Status: %s
                Start Date: %s
                End Date: %s
                """.formatted(
                policy.getPolicyNumber(),
                plan != null ? plan.getPlanName() : "N/A",
                plan != null ? plan.getDescription() : "N/A",
                plan != null ? plan.getCoverageAmount() : "N/A",
                plan != null ? plan.getPremiumAmount() : "N/A",
                policy.getStatus(),
                policy.getStartDate(),
                policy.getEndDate()
        );
    }

    @Tool(description = "Get the current status of one of the logged-in customer's insurance claims by its " +
            "claim number. Claim numbers can appear in more than one format (e.g. CLM1001 or " +
            "CLM-2026-4821) - pass whatever the customer gives you as-is, don't reject or " +
            "reformat it. Use this whenever a customer asks about the status of a specific " +
            "claim and has given you the claim number. If they haven't given a claim number, " +
            "ask them for it first instead of guessing.")
    public String getClaimStatus(
            @ToolParam(description = "The claim number exactly as given by the customer, e.g. CLM1001 or CLM-2026-4821") String claimNumber) {
        String wanted = claimNumber == null ? "" : claimNumber.trim();

        // Only claims on the logged-in user's own policies are searched. A
        // claim that belongs to someone else gets the same answer as one
        // that doesn't exist.
        Optional<Claim> found = Optional.empty();
        for (Policy policy : policyService.getPoliciesByUserId(currentUser.getId())) {
            List<Claim> claims = claimService.getClaimsByPolicyId(policy.getId());
            found = claims.stream()
                    .filter(c -> c.getClaimNumber().equalsIgnoreCase(wanted))
                    .findFirst();
            if (found.isPresent()) {
                break;
            }
        }

        if (found.isEmpty()) {
            return "No claim was found with number " + claimNumber + ". Double-check the claim number and try again.";
        }

        Claim claim = found.get();
        return """
                Claim Number: %s
                Type: %s
                Status: %s
                Amount: \u20b9%s
                Submitted: %s
                Description: %s
                """.formatted(
                claim.getClaimNumber(),
                claim.getClaimType(),
                claim.getStatus(),
                claim.getClaimAmount(),
                claim.getSubmittedAt(),
                claim.getDescription()
        );
    }

    @Tool(description = "File a new insurance claim against one of the logged-in customer's existing policies. " +
            "Use this only once you have ALL of the following from the customer: policy ID, claim type " +
            "(e.g. Hospitalization, Accident), a description of what happened, and the claim " +
            "amount. If anything is missing, ask the customer for it first instead of guessing " +
            "or filing an incomplete claim.")
    public String createClaim(
            @ToolParam(description = "The numeric policy ID this claim is for") Long policyId,
            @ToolParam(description = "Type of claim, e.g. Hospitalization, Accident") String claimType,
            @ToolParam(description = "Description of what happened") String description,
            @ToolParam(description = "The amount being claimed, in rupees") BigDecimal claimAmount) {
        if (findOwnPolicy(policyId).isEmpty()) {
            return "Couldn't file the claim - no policy was found with ID " + policyId + ".";
        }

        CreateClaimRequest request = new CreateClaimRequest();
        request.setPolicyId(policyId);
        request.setClaimType(claimType);
        request.setDescription(description);
        request.setClaimAmount(claimAmount);

        Claim created = claimService.createClaim(request);
        return "Claim filed successfully. Your claim number is " + created.getClaimNumber()
                + ". Current status: " + created.getStatus() + ".";
    }

    // Looks only inside the logged-in user's own policies.
    private Optional<Policy> findOwnPolicy(Long policyId) {
        if (policyId == null) {
            return Optional.empty();
        }
        return policyService.getPoliciesByUserId(currentUser.getId()).stream()
                .filter(p -> p.getId().equals(policyId))
                .findFirst();
    }
}