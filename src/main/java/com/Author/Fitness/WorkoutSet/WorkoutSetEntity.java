package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.WorkoutExercise.WorkoutExerciseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Builder
@Table(name = "workout_sets")
public class WorkoutSetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_exercise_id",nullable = false)
    private WorkoutExerciseEntity workoutExercise;

    @Column(name = "set_number",nullable = false)
    private Integer position;

    @Column(name = "reps_done",nullable = false)
    private Integer repsDone;

    @Column(name = "weight",nullable = false)
    private BigDecimal weight;


}
