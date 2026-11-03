package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.WorkoutSet.dto.CreateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.UpdateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WorkoutSetMapper {

    @Mapping(target = "workoutExercise", ignore = true) // подставляется в сервисе
    WorkoutSetEntity createRequestToEntity(CreateWorkoutSetRequest request);

    void updateEntityFromRequest(UpdateWorkoutSetRequest request, @MappingTarget WorkoutSetEntity entity);

    @Mapping(source = "workoutExercise.id", target = "workoutExerciseId")
    WorkoutSetResponse entityToResponse(WorkoutSetEntity entity);
}