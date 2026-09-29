package com.insurance.assistant.service;

import com.insurance.assistant.dto.CreateClaimRequest;
import com.insurance.assistant.entity.Claim;
import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.repository.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicyService policyService;

    @InjectMocks
    private ClaimService claimService;

    @Test
    void returnsClaimWhenItExists() {
        Claim claim = new Claim();
        claim.setId(1L);
        claim.setClaimNumber("CLM1001");
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        Claim result = claimService.getClaimById(1L);

        assertThat(result.getClaimNumber()).isEqualTo("CLM1001");
    }

    @Test
    void throwsWhenClaimDoesNotExist() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.getClaimById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createsClaimForAnExistingPolicy() {
        Policy policy = new Policy();
        policy.setId(1L);
        policy.setPolicyNumber("POL1001");
        when(policyService.getPolicyById(1L)).thenReturn(policy);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateClaimRequest request = new CreateClaimRequest();
        request.setPolicyId(1L);
        request.setClaimType("Hospitalization");
        request.setDescription("3 days in hospital");
        request.setClaimAmount(new BigDecimal("45000.00"));

        Claim result = claimService.createClaim(request);

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(captor.capture());

        assertThat(captor.getValue().getPolicy()).isEqualTo(policy);
        assertThat(captor.getValue().getClaimAmount()).isEqualByComparingTo("45000.00");
        assertThat(result.getClaimNumber()).startsWith("CLM-");
    }

    @Test
    void doesNotCreateClaimWhenPolicyIsMissing() {
        when(policyService.getPolicyById(99L))
                .thenThrow(new ResourceNotFoundException("Policy not found with id: 99"));

        CreateClaimRequest request = new CreateClaimRequest();
        request.setPolicyId(99L);
        request.setClaimType("Accident");
        request.setDescription("Fender bender");
        request.setClaimAmount(new BigDecimal("5000.00"));

        assertThatThrownBy(() -> claimService.createClaim(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(claimRepository, never()).save(any());
    }
}
