package com.Author.Fitness.Exercise;

import com.Author.Fitness.WorkoutExercise.WorkoutExerciseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Builder
@Table(name = "exercises")
public class ExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "muscle_group", nullable = false)
    private String muscleGroup;

    @Column(name = "equipment")
    private String equipment;

    @Column(name = "description")
    private String description;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "exercise")
    private List<WorkoutExerciseEntity> workoutExerciseEntities;



}
