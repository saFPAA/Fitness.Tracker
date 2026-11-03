package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Exercise.ExerciseEntity;
import com.Author.Fitness.Workout.WorkoutEntity;
import com.Author.Fitness.WorkoutExercise.dto.CreateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.UpdateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.WorkoutExerciseResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class WorkoutExerciseMapperTest {

    private final WorkoutExerciseMapper mapper = Mappers.getMapper(WorkoutExerciseMapper.class);

    @Test
    @DisplayName("Map a create request to an entity.")
    void shouldMapCreateRequestToEntity(){

        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(1L, 2L, 1);

        WorkoutExerciseEntity entity = mapper.createRequestToEntity(request);

        Assertions.assertNotNull(entity);
        Assertions.assertNull(entity.getWorkout());
        Assertions.assertNull(entity.getExercise());
        Assertions.assertEquals(request.orderIndex(), entity.getOrderIndex());
    }

    @Test
    @DisplayName("Map an entity to an API response.")
    void shouldMapEntityToResponse(){
        WorkoutEntity workout = new WorkoutEntity();
        workout.setId(1L);

        ExerciseEntity exercise = new ExerciseEntity();
        exercise.setId(2L);

        WorkoutExerciseEntity entity = new WorkoutExerciseEntity();
        entity.setId(10L);
        entity.setWorkout(workout);
        entity.setExercise(exercise);
        entity.setOrderIndex(1);

        WorkoutExerciseResponse response = mapper.entityToResponse(entity);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(entity.getId(), response.id());
        Assertions.assertEquals(entity.getWorkout().getId(), response.workoutId());
        Assertions.assertEquals(entity.getExercise().getId(), response.exerciseId());
        Assertions.assertEquals(entity.getOrderIndex(), response.orderIndex());
    }

    @Test
    @DisplayName("Update an entity from a request.")
    void shouldUpdateEntityFromRequest(){
        UpdateWorkoutExerciseRequest request = new UpdateWorkoutExerciseRequest(3);

        WorkoutEntity workout = new WorkoutEntity();
        workout.setId(1L);

        ExerciseEntity exercise = new ExerciseEntity();
        exercise.setId(2L);

        WorkoutExerciseEntity entity = new WorkoutExerciseEntity();
        entity.setId(10L);
        entity.setWorkout(workout);
        entity.setExercise(exercise);
        entity.setOrderIndex(1);

        WorkoutExerciseEntity entityBeforeUpdated = entity;

        mapper.updateEntityFromRequest(request, entity);

        //values
        Assertions.assertEquals(10L, entity.getId());
        Assertions.assertEquals(1L, entity.getWorkout().getId());
        Assertions.assertEquals(2L, entity.getExercise().getId());
        Assertions.assertEquals(3, entity.getOrderIndex());

        //the same
        Assertions.assertSame(entityBeforeUpdated, entity);
    }
}