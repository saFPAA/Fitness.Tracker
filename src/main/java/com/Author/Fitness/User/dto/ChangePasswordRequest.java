package com.Author.Fitness.User.dto;

import com.Author.Fitness.Exception.Validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must be no longer than 72 UTF-8 bytes")
        String newPassword
) {
}
