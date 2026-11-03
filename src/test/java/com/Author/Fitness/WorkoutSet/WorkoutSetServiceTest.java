package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.Workout.WorkoutEntity;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseEntity;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseRepository;
import com.Author.Fitness.WorkoutSet.dto.CreateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.UpdateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutSetServiceTest {

    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Mock
    private WorkoutSetRepository workoutSetRepository;

    @Mock
    private WorkoutSetMapper mapper;

    @InjectMocks
    private WorkoutSetService service;

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

    private WorkoutExerciseEntity workoutExerciseOwnedBy(Long workoutExerciseId, Long ownerId) {
        UserEntity owner = UserEntity.builder().id(ownerId).build();
        WorkoutEntity workout = WorkoutEntity.builder().id(10L).user(owner).build();
        return WorkoutExerciseEntity.builder().id(workoutExerciseId).workout(workout).build();
    }

    private WorkoutSetEntity workoutSetFor(Long id, WorkoutExerciseEntity workoutExercise) {
        return WorkoutSetEntity.builder()
                .id(id)
                .workoutExercise(workoutExercise)
                .position(1)
                .repsDone(10)
                .weight(BigDecimal.valueOf(50))
                .build();
    }

    @Test
    @DisplayName("The owner can add a set to an exercise successfully.")
    void addSetToExercise_whenOwner_createsSuccessfully() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        CreateWorkoutSetRequest request = new CreateWorkoutSetRequest(workoutExerciseId, 1, 10, BigDecimal.valueOf(50));

        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(workoutExerciseId, ownerId);
        WorkoutSetEntity mappedEntity = WorkoutSetEntity.builder().position(1).repsDone(10).weight(BigDecimal.valueOf(50)).build();
        WorkoutSetEntity savedEntity = workoutSetFor(1000L, workoutExercise);
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, workoutExerciseId, 1, 10, BigDecimal.valueOf(50));

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(workoutExercise));
        when(mapper.createRequestToEntity(request)).thenReturn(mappedEntity);
        when(workoutSetRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.entityToResponse(savedEntity)).thenReturn(response);

        WorkoutSetResponse result = service.addSetToExercise(request, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
        assertThat(mappedEntity.getWorkoutExercise()).isEqualTo(workoutExercise);
    }

    @Test
    @DisplayName("A user without permission cannot add a set to another user’s exercise.")
    void addSetToExercise_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        CreateWorkoutSetRequest request = new CreateWorkoutSetRequest(workoutExerciseId, 1, 10, BigDecimal.valueOf(50));

        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(workoutExerciseId, ownerId);
        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(workoutExercise));

        assertThatThrownBy(() -> service.addSetToExercise(request, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(workoutSetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adding a set to a nonexistent workout exercise returns a not found error.")
    void addSetToExercise_whenWorkoutExerciseNotFound_throwsEntityNotFound() {
        Long workoutExerciseId = 100L;
        CreateWorkoutSetRequest request = new CreateWorkoutSetRequest(workoutExerciseId, 1, 10, BigDecimal.valueOf(50));

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addSetToExercise(request, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("The owner can get a set by ID.")
    void getWorkoutSetById_whenOwner_returnsEntity() {
        Long setId = 1000L;
        Long ownerId = 1L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);
        WorkoutSetResponse response = new WorkoutSetResponse(setId, 100L, 1, 10, BigDecimal.valueOf(50));

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutSetResponse result = service.getWorkoutSetById(setId, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("An administrator can get any set by ID.")
    void getWorkoutSetById_whenAdmin_returnsAnyEntity() {
        Long setId = 1000L;
        Long ownerId = 1L;
        Long adminId = 999L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);
        WorkoutSetResponse response = new WorkoutSetResponse(setId, 100L, 1, 10, BigDecimal.valueOf(50));

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutSetResponse result = service.getWorkoutSetById(setId, adminDetails(adminId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("A user without permission cannot get another user’s set.")
    void getWorkoutSetById_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long setId = 1000L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getWorkoutSetById(setId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(mapper, never()).entityToResponse(any());
    }

    @Test
    @DisplayName("Getting a nonexistent set returns a not found error.")
    void getWorkoutSetById_whenNotFound_throwsEntityNotFound() {
        Long setId = 1000L;
        when(workoutSetRepository.findById(setId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkoutSetById(setId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("The owner can get the sets for a workout exercise.")
    void getSetsByWorkoutExercise_whenOwner_returnsList() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(workoutExerciseId, ownerId);
        WorkoutSetEntity entity = workoutSetFor(1000L, workoutExercise);
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, workoutExerciseId, 1, 10, BigDecimal.valueOf(50));

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(workoutExercise));
        when(workoutSetRepository.findAllByWorkoutExerciseIdOrderByPositionAsc(workoutExerciseId)).thenReturn(List.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        List<WorkoutSetResponse> result = service.getSetsByWorkoutExercise(workoutExerciseId, ownerDetails(ownerId));

        assertThat(result).containsExactly(response);
    }

    @Test
    @DisplayName("A user without permission cannot get another user’s sets.")
    void getSetsByWorkoutExercise_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(workoutExerciseId, ownerId);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(workoutExercise));

        assertThatThrownBy(() -> service.getSetsByWorkoutExercise(workoutExerciseId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(workoutSetRepository);
    }

    @Test
    @DisplayName("Getting sets for a nonexistent workout exercise returns a not found error.")
    void getSetsByWorkoutExercise_whenWorkoutExerciseNotFound_throwsEntityNotFound() {
        Long workoutExerciseId = 100L;
        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSetsByWorkoutExercise(workoutExerciseId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("The owner can update a set successfully.")
    void updateSet_whenOwner_updatesSuccessfully() {
        Long setId = 1000L;
        Long ownerId = 1L;
        UpdateWorkoutSetRequest request = new UpdateWorkoutSetRequest(2, 12, BigDecimal.valueOf(55));
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);
        WorkoutSetResponse response = new WorkoutSetResponse(setId, 100L, 2, 12, BigDecimal.valueOf(55));

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));
        when(workoutSetRepository.save(entity)).thenReturn(entity);
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutSetResponse result = service.updateSet(request, setId, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
        verify(mapper).updateEntityFromRequest(request, entity);
    }

    @Test
    @DisplayName("A user without permission cannot update another user’s set.")
    void updateSet_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long setId = 1000L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        UpdateWorkoutSetRequest request = new UpdateWorkoutSetRequest(2, 12, BigDecimal.valueOf(55));
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateSet(request, setId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(workoutSetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating a nonexistent set returns a not found error.")
    void updateSet_whenNotFound_throwsEntityNotFound() {
        Long setId = 1000L;
        UpdateWorkoutSetRequest request = new UpdateWorkoutSetRequest(2, 12, BigDecimal.valueOf(55));

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSet(request, setId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("The owner can delete a set successfully.")
    void deleteSet_whenOwner_deletesSuccessfully() {
        Long setId = 1000L;
        Long ownerId = 1L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));

        service.deleteSet(setId, ownerDetails(ownerId));

        verify(workoutSetRepository).deleteById(setId);
    }

    @Test
    @DisplayName("An administrator can delete any set.")
    void deleteSet_whenAdmin_deletesAny() {
        Long setId = 1000L;
        Long ownerId = 1L;
        Long adminId = 999L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));

        service.deleteSet(setId, adminDetails(adminId));

        verify(workoutSetRepository).deleteById(setId);
    }

    @Test
    @DisplayName("A user without permission cannot delete another user’s set.")
    void deleteSet_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long setId = 1000L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutExerciseEntity workoutExercise = workoutExerciseOwnedBy(100L, ownerId);
        WorkoutSetEntity entity = workoutSetFor(setId, workoutExercise);

        when(workoutSetRepository.findById(setId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.deleteSet(setId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(workoutSetRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deleting a nonexistent set returns a not found error.")
    void deleteSet_whenNotFound_throwsEntityNotFound() {
        Long setId = 1000L;
        when(workoutSetRepository.findById(setId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSet(setId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(workoutSetRepository, never()).deleteById(any());
    }
}
