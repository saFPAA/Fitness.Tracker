package com.Author.Fitness.WorkoutSet.dto;

import java.math.BigDecimal;

public record WorkoutSetResponse(
        Long id,
        Long workoutExerciseId,
        Integer position,
        Integer repsDone,
        BigDecimal weight
) {
}
