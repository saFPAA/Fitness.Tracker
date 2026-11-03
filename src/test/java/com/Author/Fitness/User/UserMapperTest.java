package com.Author.Fitness.User;

import com.Author.Fitness.User.dto.CreateUserRequest;
import com.Author.Fitness.User.dto.UserResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

public class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Map a create request to an entity.")
    void shouldMapCreateRequestToEntity(){

        CreateUserRequest request = new CreateUserRequest("alex", "alex@mail.com", "password123");

        UserEntity entity = mapper.createRequestToEntity(request);

        Assertions.assertNotNull(entity);
        Assertions.assertNull(entity.getPassword()); // хэшируется в сервисе
        Assertions.assertEquals(request.username(), entity.getUsername());
        Assertions.assertEquals(request.email(), entity.getEmail());
    }

    @Test
    @DisplayName("Map an entity to an API response.")
    void shouldMapEntityToResponse(){
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        entity.setUsername("alex");
        entity.setEmail("alex@mail.com");
        entity.setPassword("hashed_password");
        entity.setCreatedAt(LocalDateTime.of(2026, 9, 11, 12, 0));

        UserResponse response = mapper.entityToResponse(entity);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(entity.getId(), response.id());
        Assertions.assertEquals(entity.getUsername(), response.username());
        Assertions.assertEquals(entity.getEmail(), response.email());
        Assertions.assertEquals(entity.getCreatedAt(), response.createdAt());
    }


}