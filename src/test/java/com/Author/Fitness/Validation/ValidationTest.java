package com.Author.Fitness.Validation;

import com.Author.Fitness.User.dto.ChangePasswordRequest;
import com.Author.Fitness.User.dto.CreateUserRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Rejects a registration password shorter than 8 characters")
    void registrationPassword_shorterThan8Characters_isRejected() {
        CreateUserRequest request =
                new CreateUserRequest("alex", "alex@example.com", "short");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Rejects a password change request shorter than 8 characters")
    void passwordChange_shorterThan8Characters_isRejected() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("current-password", "short");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Rejects a registration password longer than 72 UTF-8 bytes")
    void registrationPassword_longerThan72Utf8Bytes_isRejected() {
        CreateUserRequest request =
                new CreateUserRequest("alex", "alex@example.com", "я".repeat(37));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Rejects a password change request longer than 72 UTF-8 bytes")
    void passwordChange_longerThan72Utf8Bytes_isRejected() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("current-password", "я".repeat(37));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Accepts a valid password within the UTF-8 byte limit")
    void passwordOf72Utf8Bytes_isAccepted() {
        String password = "a".repeat(72);

        CreateUserRequest registrationRequest =
                new CreateUserRequest("alex", "alex@example.com", password);
        ChangePasswordRequest changeRequest =
                new ChangePasswordRequest("current-password", password);

        assertThat(validator.validate(registrationRequest)).isEmpty();
        assertThat(validator.validate(changeRequest)).isEmpty();
    }
}