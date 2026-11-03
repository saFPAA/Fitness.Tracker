package com.Author.Fitness.WorkoutSet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSetEntity,Long> {
    List<WorkoutSetEntity> findAllByWorkoutExerciseIdOrderByPositionAsc(Long workoutExerciseId);

    @Query("SELECT ws FROM WorkoutSetEntity ws " +
            "WHERE ws.workoutExercise.exercise.id = :exerciseId " +
            "AND ws.workoutExercise.workout.user.id = :userId " +
            "ORDER BY ws.workoutExercise.workout.date ASC")
    List<WorkoutSetEntity> findProgressForExercise(Long userId, Long exerciseId);
}
