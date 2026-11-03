package com.Author.Fitness.User;

import com.Author.Fitness.User.dto.CreateUserRequest;
import com.Author.Fitness.User.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    UserEntity createRequestToEntity(CreateUserRequest request);


    UserResponse entityToResponse(UserEntity entity);
}