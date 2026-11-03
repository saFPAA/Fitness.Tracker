package com.Author.Fitness.WorkoutSet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateWorkoutSetRequest(
        @NotNull(message = "Workout exercise id is required")
        Long workoutExerciseId,

        @NotNull(message = "Set number is required")
        @Positive(message = "Set number must be positive")
        Integer position,

        @NotNull(message = "Reps done is required")
        @PositiveOrZero(message = "Reps done cannot be negative")
        Integer repsDone,

        @NotNull(message = "Weight is required")
        @PositiveOrZero(message = "Weight cannot be negative")
        BigDecimal weight
) {
}