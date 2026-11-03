package com.Author.Fitness.Workout;

import com.Author.Fitness.Workout.dto.CreateWorkoutRequest;
import com.Author.Fitness.Workout.dto.UpdateWorkoutRequest;
import com.Author.Fitness.Workout.dto.WorkoutResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WorkoutMapper {

    @Mapping(target = "user", ignore = true) // user подставляется в сервисе
    WorkoutEntity createRequestToEntity(CreateWorkoutRequest request);

    void updateEntityFromRequest(UpdateWorkoutRequest request, @MappingTarget WorkoutEntity entity);

    @Mapping(source = "user.id", target = "userId")
    WorkoutResponse entityToResponse(WorkoutEntity entity);
}