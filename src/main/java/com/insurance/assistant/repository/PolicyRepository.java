package com.insurance.assistant.repository;

import com.insurance.assistant.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByUserId(Long userId);

    Optional<Policy> findByPolicyNumber(String policyNumber);
}
