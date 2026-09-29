package com.insurance.assistant.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateClaimRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void rejectsZeroOrNegativeClaimAmount() {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setPolicyId(1L);
        request.setClaimType("Hospitalization");
        request.setDescription("Some description");
        request.setClaimAmount(new BigDecimal("0.00"));

        Set<ConstraintViolation<CreateClaimRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("claimAmount"));
    }

    @Test
    void rejectsBlankDescription() {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setPolicyId(1L);
        request.setClaimType("Hospitalization");
        request.setDescription("  ");
        request.setClaimAmount(new BigDecimal("1000.00"));

        Set<ConstraintViolation<CreateClaimRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void acceptsAValidRequest() {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setPolicyId(1L);
        request.setClaimType("Hospitalization");
        request.setDescription("3-day hospitalization");
        request.setClaimAmount(new BigDecimal("45000.00"));

        assertThat(validator.validate(request)).isEmpty();
    }
}
