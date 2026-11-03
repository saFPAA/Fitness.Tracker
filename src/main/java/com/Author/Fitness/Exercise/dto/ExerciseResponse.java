package com.Author.Fitness.Exercise.dto;

public record ExerciseResponse(
        Long id,
        String name,
        String muscleGroup,
        String equipment,
        String description,
        String videoUrl,
        String imageUrl
) {
}
