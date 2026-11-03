package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Exercise.ExerciseEntity;
import com.Author.Fitness.Workout.WorkoutEntity;
import com.Author.Fitness.WorkoutSet.WorkoutSetEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Builder
@Table(name = "workout_exercises",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_workout_order",
                columnNames = {"workout_id", "order_index"}
        ))
public class WorkoutExerciseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private WorkoutEntity workout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @OneToMany(
            mappedBy = "workoutExercise",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<WorkoutSetEntity> workoutSets;
}
