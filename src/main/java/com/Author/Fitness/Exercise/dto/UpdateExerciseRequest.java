package com.Author.Fitness.Exercise.dto;

public record UpdateExerciseRequest(
        String name,
        String muscleGroup,
        String equipment,
        String description,
        String videoUrl,
        String imageUrl
) {
}
