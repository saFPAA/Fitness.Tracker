package com.Author.Fitness.User.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt
) {
}