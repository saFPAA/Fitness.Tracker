package com.Author.Fitness.Workout.dto;

import java.time.LocalDate;

public record WorkoutResponse(
        Long id,
        String name,
        LocalDate date,
        Long userId
) {

}
