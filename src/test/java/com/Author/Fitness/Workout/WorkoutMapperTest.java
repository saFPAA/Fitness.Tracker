package com.Author.Fitness.Workout;

import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.Workout.dto.CreateWorkoutRequest;
import com.Author.Fitness.Workout.dto.UpdateWorkoutRequest;
import com.Author.Fitness.Workout.dto.WorkoutResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

public class WorkoutMapperTest {

    private final WorkoutMapper mapper = Mappers.getMapper(WorkoutMapper.class);

    @Test
    @DisplayName("Map a create request to an entity.")
    void shouldMapCreateRequestToEntity(){

        CreateWorkoutRequest request = new CreateWorkoutRequest("Push day", LocalDate.of(2026, 9, 11), 1L);

        WorkoutEntity entity = mapper.createRequestToEntity(request);

        Assertions.assertNotNull(entity);
        Assertions.assertNull(entity.getUser());
        Assertions.assertEquals(request.name(), entity.getName());
        Assertions.assertEquals(request.date(), entity.getDate());
    }

    @Test
    @DisplayName("Map an entity to an API response.")
    void shouldMapEntityToResponse(){
        UserEntity user = new UserEntity();
        user.setId(5L);

        WorkoutEntity entity = new WorkoutEntity();
        entity.setId(1L);
        entity.setName("Push day");
        entity.setDate(LocalDate.of(2026, 9, 11));
        entity.setUser(user);

        WorkoutResponse response = mapper.entityToResponse(entity);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(entity.getId(), response.id());
        Assertions.assertEquals(entity.getName(), response.name());
        Assertions.assertEquals(entity.getDate(), response.date());
        Assertions.assertEquals(entity.getUser().getId(), response.userId());
    }

    @Test
    @DisplayName("Update an entity from a request.")
    void shouldUpdateEntityFromRequest(){
        UpdateWorkoutRequest request = new UpdateWorkoutRequest("Pull day", LocalDate.of(2026, 9, 12));

        UserEntity user = new UserEntity();
        user.setId(5L);

        WorkoutEntity entity = new WorkoutEntity();
        entity.setId(1L);
        entity.setName("Push day");
        entity.setDate(LocalDate.of(2026, 9, 11));
        entity.setUser(user);

        WorkoutEntity entityBeforeUpdated = entity;

        mapper.updateEntityFromRequest(request, entity);

        //values
        Assertions.assertEquals(1L, entity.getId());
        Assertions.assertEquals(5L, entity.getUser().getId());
        Assertions.assertEquals("Pull day", entity.getName());
        Assertions.assertEquals(LocalDate.of(2026, 9, 12), entity.getDate());

        //the same
        Assertions.assertSame(entityBeforeUpdated, entity);
    }
}