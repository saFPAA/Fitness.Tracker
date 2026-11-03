package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.WorkoutExercise.dto.CreateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.UpdateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.WorkoutExerciseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WorkoutExerciseMapper {

    @Mapping(target = "workout", ignore = true)  // подставляется в сервисе
    @Mapping(target = "exercise", ignore = true) // подставляется в сервисе
    WorkoutExerciseEntity createRequestToEntity(CreateWorkoutExerciseRequest request);

    void updateEntityFromRequest(UpdateWorkoutExerciseRequest request, @MappingTarget WorkoutExerciseEntity entity);

    @Mapping(source = "workout.id", target = "workoutId")
    @Mapping(source = "exercise.id", target = "exerciseId")
    WorkoutExerciseResponse entityToResponse(WorkoutExerciseEntity entity);
}