package com.Author.Fitness.Workout;

import com.Author.Fitness.User.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutRepository extends JpaRepository<WorkoutEntity,Long> {
    Page<WorkoutEntity> findAllByUserIdOrderByDateDesc(Long userId, Pageable pageable);
}
