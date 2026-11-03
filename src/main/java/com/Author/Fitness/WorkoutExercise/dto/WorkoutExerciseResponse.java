package com.Author.Fitness.WorkoutExercise.dto;

public record WorkoutExerciseResponse(
        Long id,
        Long workoutId,
        Long exerciseId,
        Integer orderIndex
) {
}