package com.Author.Fitness.Workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateWorkoutRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "date is required")
        LocalDate date,
        @NotNull(message = "user id os required")
        Long userId
) {
}
