package com.Author.Fitness.Auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}