package com.insurance.assistant.service;

import com.insurance.assistant.entity.Policy;
import com.insurance.assistant.exception.ResourceNotFoundException;
import com.insurance.assistant.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    @Test
    void returnsPolicyWhenItExists() {
        Policy policy = new Policy();
        policy.setId(1L);
        policy.setPolicyNumber("POL1001");
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        Policy result = policyService.getPolicyById(1L);

        assertThat(result.getPolicyNumber()).isEqualTo("POL1001");
    }

    @Test
    void throwsWhenPolicyDoesNotExist() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicyById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void returnsPoliciesForAUser() {
        Policy policy = new Policy();
        policy.setId(1L);
        policy.setPolicyNumber("POL1001");
        when(policyRepository.findByUserId(1L)).thenReturn(List.of(policy));

        List<Policy> result = policyService.getPoliciesByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPolicyNumber()).isEqualTo("POL1001");
    }

    @Test
    void returnsEmptyListWhenUserHasNoPolicies() {
        when(policyRepository.findByUserId(2L)).thenReturn(List.of());

        assertThat(policyService.getPoliciesByUserId(2L)).isEmpty();
    }
}
