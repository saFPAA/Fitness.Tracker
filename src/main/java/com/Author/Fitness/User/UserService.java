package com.Author.Fitness.User;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.SecurityUtils;
import com.Author.Fitness.User.dto.CreateUserRequest;
import com.Author.Fitness.User.dto.UserResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;



    @Transactional
    public UserResponse createUser(CreateUserRequest request){
        if (userRepository.existsByUsername(request.username())) {
            log.error("Failed to create user: username '{}' already taken", request.username());
            throw new EntityExistsException("Username already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            log.error("Failed to create user: email '{}' already registered", request.email());
            throw new EntityExistsException("Email already registered");
        }

        UserEntity entity = mapper.createRequestToEntity(request);
        entity.setPassword(passwordEncoder.encode(request.password()));
        entity.setRole(Role.USER);

        UserEntity createdUser = userRepository.save(entity);
        log.info("User created with id: {}", createdUser.getId());
        return mapper.entityToResponse(createdUser);
    }

    public UserResponse getUserById(Long id, CustomUserDetails currentUser){
        SecurityUtils.checkOwnershipOrAdmin(id, currentUser);

        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find user with id: {} during get by id", id);
                    return new EntityNotFoundException("User not found");
                });
        return mapper.entityToResponse(entity);
    }


    @Transactional
    public void deleteUser(Long id, CustomUserDetails currentUser){
        SecurityUtils.checkOwnershipOrAdmin(id, currentUser);

        if(!userRepository.existsById(id)){
            log.error("Failed to find user with id: {} during delete", id);
            throw new EntityNotFoundException("User not found");
        }

        userRepository.deleteById(id);
        log.info("User deleted with id: {}", id);
    }
}