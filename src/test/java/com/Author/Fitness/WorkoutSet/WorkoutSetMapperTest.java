package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.WorkoutExercise.WorkoutExerciseEntity;
import com.Author.Fitness.WorkoutSet.dto.CreateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.UpdateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

public class WorkoutSetMapperTest {

    private final WorkoutSetMapper mapper = Mappers.getMapper(WorkoutSetMapper.class);

    @Test
    @DisplayName("Map a create request to an entity.")
    void shouldMapCreateRequestToEntity(){

        CreateWorkoutSetRequest request = new CreateWorkoutSetRequest(1L,2,8, BigDecimal.valueOf(65.5));

        WorkoutSetEntity entity = mapper.createRequestToEntity(request);

        Assertions.assertNotNull(entity);
        Assertions.assertNull(entity.getWorkoutExercise());
        Assertions.assertEquals(request.position(),entity.getPosition());
        Assertions.assertEquals(request.repsDone(),entity.getRepsDone());
        Assertions.assertEquals(request.weight(),entity.getWeight());
    }

    @Test
    @DisplayName("Map an entity to an API response.")
    void shouldMapEntityToResponse(){
        WorkoutExerciseEntity exerciseEntity = new WorkoutExerciseEntity();
        exerciseEntity.setId(1L);

        WorkoutSetEntity entity = new WorkoutSetEntity();
        entity.setId(1L);
        entity.setWorkoutExercise(exerciseEntity);
        entity.setPosition(2);
        entity.setRepsDone(8);
        entity.setWeight(BigDecimal.valueOf(65.5));

        WorkoutSetResponse response = mapper.entityToResponse(entity);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(entity.getId(),response.id());
        Assertions.assertEquals(entity.getWorkoutExercise().getId(),response.workoutExerciseId());
        Assertions.assertEquals(entity.getPosition(),response.position());
        Assertions.assertEquals(entity.getRepsDone(),response.repsDone());
        Assertions.assertEquals(entity.getWeight(),response.weight());
    }

    @Test
    @DisplayName("Update an entity from a request.")
    void shouldUpdateEntityFromRequest(){
        UpdateWorkoutSetRequest request = new UpdateWorkoutSetRequest(3,8,BigDecimal.valueOf(75.5));

        WorkoutExerciseEntity exerciseEntity = new WorkoutExerciseEntity();
        exerciseEntity.setId(1L);

        WorkoutSetEntity entity = new WorkoutSetEntity();
        entity.setId(1L);
        entity.setWorkoutExercise(exerciseEntity);
        entity.setPosition(2);
        entity.setRepsDone(8);
        entity.setWeight(BigDecimal.valueOf(65.5));

        WorkoutSetEntity entityBeforeUpdated = entity;

        mapper.updateEntityFromRequest(request,entity);

        //values
        Assertions.assertEquals(1L,entity.getId());
        Assertions.assertEquals(1L,entity.getWorkoutExercise().getId());
        Assertions.assertEquals(3,entity.getPosition());
        Assertions.assertEquals(8,entity.getRepsDone());
        Assertions.assertEquals(BigDecimal.valueOf(75.5),entity.getWeight());

        //the same
        Assertions.assertSame(entityBeforeUpdated,entity);
    }

}
