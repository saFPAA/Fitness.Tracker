package com.Author.Fitness.Exercise;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<ExerciseEntity,Long> {

    Page<ExerciseEntity> findAllByMuscleGroup(String MuscleGroup, Pageable pageable);

    Page<ExerciseEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

}
