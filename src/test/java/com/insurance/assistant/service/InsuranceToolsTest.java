package com.insurance.assistant.service;

import com.insurance.assistant.dto.CreateClaimRequest;
import com.insurance.assistant.entity.Claim;
import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceToolsTest {

    private static final Long LOGGED_IN_USER_ID = 4L;
    private static final Long OWN_POLICY_ID = 3L;
    private static final Long OTHER_USERS_POLICY_ID = 1L;

    @Mock
    private PolicyService policyService;
    @Mock
    private ClaimService claimService;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private Policy ownPolicy;

    private InsuranceTools tools;

    @BeforeEach
    void setUp() {
        tools = new InsuranceTools(policyService, claimService, currentUser);
        // The logged-in user (id 4) owns exactly one policy (id 3).
        when(currentUser.getId()).thenReturn(LOGGED_IN_USER_ID);
        when(policyService.getPoliciesByUserId(LOGGED_IN_USER_ID)).thenReturn(List.of(ownPolicy));
        when(ownPolicy.getId()).thenReturn(OWN_POLICY_ID);
    }

    @Test
    void getPolicyDetails_returnsOwnPolicy() {
        when(ownPolicy.getPolicyNumber()).thenReturn("POL1003");

        String result = tools.getPolicyDetails(OWN_POLICY_ID);

        assertTrue(result.contains("POL1003"));
    }

    @Test
    void getPolicyDetails_saysNotFound_forSomeoneElsesPolicy() {
        String result = tools.getPolicyDetails(OTHER_USERS_POLICY_ID);

        assertTrue(result.contains("No policy was found with ID 1"));
    }

    @Test
    void getClaimStatus_saysNotFound_forSomeoneElsesClaim() {
        // The logged-in user's own policy has no claims, so CLM1001
        // (which belongs to another user) can't be found.
        when(claimService.getClaimsByPolicyId(OWN_POLICY_ID)).thenReturn(List.of());

        String result = tools.getClaimStatus("CLM1001");

        assertTrue(result.contains("No claim was found with number CLM1001"));
    }

    @Test
    void createClaim_refusesSomeoneElsesPolicy_andSavesNothing() {
        String result = tools.createClaim(OTHER_USERS_POLICY_ID, "Accident", "test", new BigDecimal("500"));

        assertTrue(result.contains("no policy was found with ID 1"));
        verify(claimService, never()).createClaim(any(CreateClaimRequest.class));
    }

    @Test
    void createClaim_filesTheClaim_onOwnPolicy() {
        Claim created = org.mockito.Mockito.mock(Claim.class);
        when(created.getClaimNumber()).thenReturn("CLM-2026-1234");
        when(claimService.createClaim(any(CreateClaimRequest.class))).thenReturn(created);

        String result = tools.createClaim(OWN_POLICY_ID, "Accident", "test", new BigDecimal("500"));

        assertTrue(result.contains("CLM-2026-1234"));
        verify(claimService).createClaim(any(CreateClaimRequest.class));
    }
}