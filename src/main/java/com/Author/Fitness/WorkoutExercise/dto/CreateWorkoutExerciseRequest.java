package com.Author.Fitness.WorkoutExercise.dto;

import jakarta.validation.constraints.NotNull;

public record CreateWorkoutExerciseRequest(
        @NotNull(message = "Workout id is required")
        Long workoutId,

        @NotNull(message = "Exercise id is required")
        Long exerciseId,

        @NotNull(message = "Order index is required")
        Integer orderIndex
) {
}