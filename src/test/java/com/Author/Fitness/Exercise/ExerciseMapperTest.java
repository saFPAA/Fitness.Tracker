package com.Author.Fitness.Exercise;


import com.Author.Fitness.Exercise.dto.CreateExerciseRequest;
import com.Author.Fitness.Exercise.dto.ExerciseResponse;
import com.Author.Fitness.Exercise.dto.UpdateExerciseRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class ExerciseMapperTest {

    private final ExerciseMapper mapper = Mappers.getMapper(ExerciseMapper.class);

    @Test
    @DisplayName("Map create request to an entity")
    void shouldMapCreateRequestToEntity(){

        CreateExerciseRequest request = new CreateExerciseRequest("Жим лёжа", "Грудь", "Штанга",
                "Описание", null, null);

        ExerciseEntity entity = mapper.createRequestToEntity(request);

        Assertions.assertNotNull(entity);
        Assertions.assertEquals(request.name(),entity.getName());
        Assertions.assertEquals(request.muscleGroup(),entity.getMuscleGroup());
        Assertions.assertEquals(request.equipment(),entity.getEquipment());
        Assertions.assertEquals(request.description(),entity.getDescription());
        Assertions.assertEquals(request.videoUrl(),entity.getVideoUrl());
        Assertions.assertEquals(request.imageUrl(),entity.getImageUrl());
    }

    @Test
    @DisplayName("Map an entity to an API response")
    void shouldMapEntityToResponse(){

        ExerciseEntity entity = new ExerciseEntity();
        entity.setId(1L);
        entity.setName("Жим лежа");
        entity.setMuscleGroup("Грудь");
        entity.setEquipment("Штанга");
        entity.setDescription("Описание");
        entity.setVideoUrl(null);
        entity.setImageUrl(null);

        ExerciseResponse response = mapper.entityToResponse(entity);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(entity.getId(),response.id());
        Assertions.assertEquals(entity.getName(),response.name());
        Assertions.assertEquals(entity.getDescription(),response.description());
        Assertions.assertEquals(entity.getMuscleGroup(),response.muscleGroup());
        Assertions.assertEquals(entity.getEquipment(),response.equipment());
        Assertions.assertEquals(entity.getVideoUrl(),response.videoUrl());
        Assertions.assertEquals(entity.getImageUrl(),response.imageUrl());
    }

    @Test
    @DisplayName("Update an entity from a request")
    void shouldUpdateEntityFromRequest(){
        ExerciseEntity entity = new ExerciseEntity();
        entity.setId(1L);
        entity.setName("Жим лежа");
        entity.setMuscleGroup("Грудь");
        entity.setEquipment("Штанга");
        entity.setDescription("Описание");
        entity.setVideoUrl(null);
        entity.setImageUrl(null);

        ExerciseEntity originalReference = entity;

        UpdateExerciseRequest request = new UpdateExerciseRequest("Жим лёжа измененный", "Грудь измененное",
                "Штанга изменилось",
                "Описание изменилось", "source 1", "source 2");

        mapper.updateEntityFromRequest(request, entity);

        Assertions.assertEquals(1L,entity.getId());
        Assertions.assertEquals("Жим лёжа измененный", entity.getName());
        Assertions.assertEquals("Грудь измененное", entity.getMuscleGroup());
        Assertions.assertEquals("Штанга изменилось", entity.getEquipment());
        Assertions.assertEquals("Описание изменилось", entity.getDescription());
        Assertions.assertEquals("source 1", entity.getVideoUrl());
        Assertions.assertEquals("source 2", entity.getImageUrl());

        Assertions.assertSame(originalReference, entity);
    }

}
