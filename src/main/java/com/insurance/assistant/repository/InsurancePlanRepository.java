package com.insurance.assistant.repository;

import com.insurance.assistant.entity.InsurancePlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, Long> {
}
