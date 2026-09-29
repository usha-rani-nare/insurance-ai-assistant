package com.insurance.assistant.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatRequestValidationTest {

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
    void rejectsBlankQuestion() {
        AiChatRequest request = new AiChatRequest();
        request.setQuestion("   ");

        Set<ConstraintViolation<AiChatRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("question"));
    }

    @Test
    void rejectsNullQuestion() {
        AiChatRequest request = new AiChatRequest();

        Set<ConstraintViolation<AiChatRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("question"));
    }

    @Test
    void rejectsQuestionOverTheLengthLimit() {
        AiChatRequest request = new AiChatRequest();
        request.setQuestion("a".repeat(501));

        Set<ConstraintViolation<AiChatRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("question"));
    }

    @Test
    void acceptsAReasonableQuestion() {
        AiChatRequest request = new AiChatRequest();
        request.setQuestion("Does my policy cover hospitalization?");

        assertThat(validator.validate(request)).isEmpty();
    }
}
