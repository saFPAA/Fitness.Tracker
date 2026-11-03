package com.Author.Fitness.User.dto;

import com.Author.Fitness.Exception.Validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 255)
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must be no longer than 72 UTF-8 bytes")
        String password
) {
}