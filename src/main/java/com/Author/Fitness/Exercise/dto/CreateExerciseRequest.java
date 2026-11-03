package com.Author.Fitness.Exercise.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateExerciseRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "muscle group is required")
        String muscleGroup,
        String equipment,
        String description,
        String videoUrl,
        String imageUrl
) {
}
