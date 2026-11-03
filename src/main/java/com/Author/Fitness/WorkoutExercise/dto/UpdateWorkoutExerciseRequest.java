package com.Author.Fitness.WorkoutExercise.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateWorkoutExerciseRequest(
        @NotNull(message = "Order index is required")
        Integer orderIndex
) {
}