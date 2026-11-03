package com.Author.Fitness.User;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.dto.CreateUserRequest;
import com.Author.Fitness.User.dto.UserResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    private CustomUserDetails ownerDetails(Long id) {
        UserEntity entity = UserEntity.builder()
                .id(id)
                .username("owner")
                .password("hashed")
                .role(Role.USER)
                .build();
        return new CustomUserDetails(entity);
    }

    private CustomUserDetails adminDetails(Long id) {
        UserEntity entity = UserEntity.builder()
                .id(id)
                .username("admin")
                .password("hashed")
                .role(Role.ADMIN)
                .build();
        return new CustomUserDetails(entity);
    }


    @Test
    @DisplayName("The owner can get their profile by ID.")
    void getUserById_whenOwner_returnsUser() {
        Long userId = 1L;
        UserEntity entity = UserEntity.builder().id(userId).username("owner").build();
        UserResponse response = new UserResponse(userId, "owner", "owner@test.com", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        UserResponse result = service.getUserById(userId, ownerDetails(userId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("An administrator can get any user’s profile by ID.")
    void getUserById_whenAdmin_returnsAnyUser() {
        Long userId = 1L;
        Long adminId = 999L;
        UserEntity entity = UserEntity.builder().id(userId).username("someone").build();
        UserResponse response = new UserResponse(userId, "someone", "someone@test.com", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        UserResponse result = service.getUserById(userId, adminDetails(adminId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("A user without permission is denied access to a profile.")
    void getUserById_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long userId = 1L;
        Long strangerId = 2L;

        assertThatThrownBy(() -> service.getUserById(userId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("A nonexistent user returns a not found error.")
    void getUserById_whenUserNotFound_throwsEntityNotFound() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(userId, ownerDetails(userId)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Creating a user with an existing username returns a conflict.")
    void createUser_whenUsernameTaken_throwsEntityExists() {
        CreateUserRequest request = new CreateUserRequest("taken", "new@test.com", "password123");
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating a user with an existing email returns a conflict.")
    void createUser_whenEmailTaken_throwsEntityExists() {
        CreateUserRequest request = new CreateUserRequest("newuser", "taken@test.com", "password123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating a user hashes the password and assigns the default role.")
    void createUser_success_hashesPasswordAndSetsDefaultRole() {
        CreateUserRequest request = new CreateUserRequest("newuser", "new@test.com", "password123");
        UserEntity mappedEntity = UserEntity.builder().username("newuser").email("new@test.com").build();
        UserEntity savedEntity = UserEntity.builder().id(1L).username("newuser").email("new@test.com")
                .password("hashed-password").role(Role.USER).build();
        UserResponse response = new UserResponse(1L, "newuser", "new@test.com", null);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(mapper.createRequestToEntity(request)).thenReturn(mappedEntity);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.entityToResponse(savedEntity)).thenReturn(response);

        UserResponse result = service.createUser(request);

        assertThat(result).isEqualTo(response);
        assertThat(mappedEntity.getRole()).isEqualTo(Role.USER);
        assertThat(mappedEntity.getPassword()).isEqualTo("hashed-password");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("The owner can delete their account successfully.")
    void deleteUser_whenOwner_deletesSuccessfully() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        service.deleteUser(userId, ownerDetails(userId));

        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("An administrator can delete any user.")
    void deleteUser_whenAdmin_deletesAnyUser() {
        Long userId = 1L;
        Long adminId = 999L;
        when(userRepository.existsById(userId)).thenReturn(true);

        service.deleteUser(userId, adminDetails(adminId));

        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("A user without permission cannot delete another user.")
    void deleteUser_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long userId = 1L;
        Long strangerId = 2L;

        assertThatThrownBy(() -> service.deleteUser(userId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Deleting a nonexistent user returns a not found error.")
    void deleteUser_whenUserNotFound_throwsEntityNotFound() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteUser(userId, ownerDetails(userId)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }
}
