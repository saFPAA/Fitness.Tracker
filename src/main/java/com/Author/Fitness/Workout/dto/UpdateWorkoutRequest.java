package com.Author.Fitness.Workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateWorkoutRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "date is required")
        LocalDate date
) {

}
