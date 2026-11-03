package com.Author.Fitness.Workout;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.User.UserRepository;
import com.Author.Fitness.Workout.dto.CreateWorkoutRequest;
import com.Author.Fitness.Workout.dto.UpdateWorkoutRequest;
import com.Author.Fitness.Workout.dto.WorkoutResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutMapper mapper;

    @Mock
    private WorkoutRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkoutService service;

    private CustomUserDetails userDetails(Long id, Role role) {
        UserEntity entity = UserEntity.builder()
                .id(id)
                .username("user" + id)
                .password("hashed")
                .role(role)
                .build();
        return new CustomUserDetails(entity);
    }

    private CustomUserDetails ownerDetails(Long id) {
        return userDetails(id, Role.USER);
    }

    private CustomUserDetails adminDetails(Long id) {
        return userDetails(id, Role.ADMIN);
    }

    private WorkoutEntity workoutOwnedBy(Long workoutId, Long ownerId) {
        UserEntity owner = UserEntity.builder().id(ownerId).build();
        return WorkoutEntity.builder()
                .id(workoutId)
                .name("Push day")
                .date(LocalDate.now())
                .user(owner)
                .build();
    }

    @Test
    @DisplayName("The owner can get a workout by ID.")
    void getWorkoutById_whenOwner_returnsWorkout() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);
        WorkoutResponse response = new WorkoutResponse(workoutId, "Push day", LocalDate.now(), ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutResponse result = service.getWorkoutById(workoutId, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("An administrator can get any workout by ID.")
    void getWorkoutById_whenAdmin_returnsAnyWorkout() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        Long adminId = 999L;
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);
        WorkoutResponse response = new WorkoutResponse(workoutId, "Push day", LocalDate.now(), ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutResponse result = service.getWorkoutById(workoutId, adminDetails(adminId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("A user without permission is denied access to a workout.")
    void getWorkoutById_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getWorkoutById(workoutId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository).findById(workoutId);
        verify(mapper, never()).entityToResponse(any());
    }

    @Test
    @DisplayName("Getting a nonexistent workout returns a not found error.")
    void getWorkoutById_whenWorkoutNotFound_throwsEntityNotFound() {
        Long workoutId = 10L;
        when(repository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkoutById(workoutId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("The owner can get their workouts as a page.")
    void getWorkoutsByUser_whenOwner_returnsPage() {
        Long ownerId = 1L;
        WorkoutEntity entity = workoutOwnedBy(10L, ownerId);
        WorkoutResponse response = new WorkoutResponse(10L, "Push day", LocalDate.now(), ownerId);
        Page<WorkoutEntity> page = new PageImpl<>(List.of(entity));

        when(repository.findAllByUserIdOrderByDateDesc(eq(ownerId), any(PageRequest.class))).thenReturn(page);
        when(mapper.entityToResponse(entity)).thenReturn(response);

        Page<WorkoutResponse> result = service.getWorkoutsByUser(ownerId, 0, 10, ownerDetails(ownerId));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("A user without permission cannot list another user’s workouts.")
    void getWorkoutsByUser_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long ownerId = 1L;
        Long strangerId = 2L;

        assertThatThrownBy(() -> service.getWorkoutsByUser(ownerId, 0, 10, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(repository);
    }


    @Test
    @DisplayName("The owner can create a workout successfully.")
    void createWorkout_whenOwner_createsSuccessfully() {
        Long ownerId = 1L;
        CreateWorkoutRequest request = new CreateWorkoutRequest("Push day", LocalDate.now(), ownerId);
        UserEntity owner = UserEntity.builder().id(ownerId).build();
        WorkoutEntity mappedEntity = WorkoutEntity.builder().name("Push day").date(LocalDate.now()).build();
        WorkoutEntity savedEntity = workoutOwnedBy(10L, ownerId);
        WorkoutResponse response = new WorkoutResponse(10L, "Push day", LocalDate.now(), ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(mapper.createRequestToEntity(request)).thenReturn(mappedEntity);
        when(repository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.entityToResponse(savedEntity)).thenReturn(response);

        WorkoutResponse result = service.createWorkout(request, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
        assertThat(mappedEntity.getUser()).isEqualTo(owner);
    }

    @Test
    @DisplayName("A user who is not the owner or an administrator cannot create a workout for another user.")
    void createWorkout_whenRequestUserIdNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long targetUserId = 1L;
        Long strangerId = 2L;
        CreateWorkoutRequest request = new CreateWorkoutRequest("Push day", LocalDate.now(), targetUserId);

        assertThatThrownBy(() -> service.createWorkout(request, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userRepository, repository);
    }

    @Test
    @DisplayName("Creating a workout for a nonexistent user returns a not found error.")
    void createWorkout_whenUserNotFound_throwsEntityNotFound() {
        Long ownerId = 1L;
        CreateWorkoutRequest request = new CreateWorkoutRequest("Push day", LocalDate.now(), ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWorkout(request, ownerDetails(ownerId)))
                .isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    @DisplayName("The owner can update a workout successfully.")
    void updateWorkout_whenOwner_updatesSuccessfully() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        UpdateWorkoutRequest request = new UpdateWorkoutRequest("Updated name", LocalDate.now());
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);
        WorkoutResponse response = new WorkoutResponse(workoutId, "Updated name", LocalDate.now(), ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutResponse result = service.updateWorkout(request, workoutId, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
        verify(mapper).updateEntityFromRequest(request, entity);
    }

    @Test
    @DisplayName("A user without permission cannot update a workout.")
    void updateWorkout_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        UpdateWorkoutRequest request = new UpdateWorkoutRequest("Updated name", LocalDate.now());
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateWorkout(request, workoutId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).save(any());
    }


    @Test
    @DisplayName("The owner can delete a workout successfully.")
    void deleteWorkout_whenOwner_deletesSuccessfully() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));

        service.deleteWorkout(workoutId, ownerDetails(ownerId));

        verify(repository).deleteById(workoutId);
    }

    @Test
    @DisplayName("An administrator can delete any workout.")
    void deleteWorkout_whenAdmin_deletesAnyWorkout() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        Long adminId = 999L;
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));

        service.deleteWorkout(workoutId, adminDetails(adminId));

        verify(repository).deleteById(workoutId);
    }

    @Test
    @DisplayName("A user without permission cannot delete another user’s workout.")
    void deleteWorkout_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutEntity entity = workoutOwnedBy(workoutId, ownerId);

        when(repository.findById(workoutId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.deleteWorkout(workoutId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deleting a nonexistent workout returns a not found error.")
    void deleteWorkout_whenWorkoutNotFound_throwsEntityNotFound() {
        Long workoutId = 10L;
        when(repository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteWorkout(workoutId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}
