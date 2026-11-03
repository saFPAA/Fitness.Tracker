package com.Author.Fitness.WorkoutExercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExerciseEntity,Long> {
    List<WorkoutExerciseEntity> findAllByWorkoutIdOrderByOrderIndexAsc(Long workoutId);

    boolean existsByWorkoutIdAndOrderIndex(Long workoutId, Integer orderIndex);

    boolean existsByExerciseId(Long exerciseId);
}