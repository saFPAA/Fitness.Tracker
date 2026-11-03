package com.Author.Fitness.Exercise;

import com.Author.Fitness.Exercise.dto.CreateExerciseRequest;
import com.Author.Fitness.Exercise.dto.ExerciseResponse;
import com.Author.Fitness.Exercise.dto.UpdateExerciseRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExerciseMapper {

    ExerciseEntity createRequestToEntity(CreateExerciseRequest request);

    void updateEntityFromRequest(UpdateExerciseRequest request, @MappingTarget ExerciseEntity entity);

    ExerciseResponse entityToResponse(ExerciseEntity entity);
}