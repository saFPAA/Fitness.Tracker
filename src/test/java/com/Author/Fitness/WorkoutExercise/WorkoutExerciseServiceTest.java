package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Exercise.ExerciseEntity;
import com.Author.Fitness.Exercise.ExerciseRepository;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.Workout.WorkoutEntity;
import com.Author.Fitness.Workout.WorkoutRepository;
import com.Author.Fitness.WorkoutExercise.dto.CreateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.UpdateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.WorkoutExerciseResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutExerciseServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Mock
    private WorkoutExerciseMapper mapper;

    @InjectMocks
    private WorkoutExerciseService service;

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
        return WorkoutEntity.builder().id(workoutId).user(owner).build();
    }

    private WorkoutExerciseEntity workoutExerciseFor(Long id, WorkoutEntity workout) {
        return WorkoutExerciseEntity.builder()
                .id(id)
                .workout(workout)
                .orderIndex(1)
                .build();
    }


    @Test
    @DisplayName("The owner can get a workout exercise by ID.")
    void getWorkoutExerciseById_whenOwner_returnsEntity() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(workoutExerciseId, 10L, 5L, 1);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutExerciseResponse result = service.getWorkoutExerciseById(workoutExerciseId, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("A user without permission cannot get a workout exercise.")
    void getWorkoutExerciseById_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getWorkoutExerciseById(workoutExerciseId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(mapper, never()).entityToResponse(any());
    }

    @Test
    @DisplayName("An administrator can get any workout exercise.")
    void getWorkoutExerciseById_whenAdmin_returnsAnyEntity() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        Long adminId = 999L;
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(workoutExerciseId, 10L, 5L, 1);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutExerciseResponse result = service.getWorkoutExerciseById(workoutExerciseId, adminDetails(adminId));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Getting a nonexistent workout exercise returns a not found error.")
    void getWorkoutExerciseById_whenNotFound_throwsEntityNotFound() {
        Long workoutExerciseId = 100L;
        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkoutExerciseById(workoutExerciseId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    @DisplayName("The owner can get the exercises in a workout.")
    void getExercisesByWorkout_whenOwner_returnsList() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        WorkoutEntity workout = workoutOwnedBy(workoutId, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(100L, workout);
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(100L, workoutId, 5L, 1);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.findAllByWorkoutIdOrderByOrderIndexAsc(workoutId)).thenReturn(List.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        List<WorkoutExerciseResponse> result = service.getExercisesByWorkout(workoutId, ownerDetails(ownerId));

        assertThat(result).containsExactly(response);
    }

    @Test
    @DisplayName("A user without permission cannot get another user’s workout exercises.")
    void getExercisesByWorkout_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutId = 10L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutEntity workout = workoutOwnedBy(workoutId, ownerId);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> service.getExercisesByWorkout(workoutId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(workoutExerciseRepository);
    }

    @Test
    @DisplayName("Getting exercises for a nonexistent workout returns a not found error.")
    void getExercisesByWorkout_whenWorkoutNotFound_throwsEntityNotFound() {
        Long workoutId = 10L;
        when(workoutRepository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExercisesByWorkout(workoutId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    @DisplayName("The owner can add an exercise to a workout successfully.")
    void addExerciseToWorkout_whenOwner_createsSuccessfully() {
        Long workoutId = 10L;
        Long exerciseId = 5L;
        Long ownerId = 1L;
        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(workoutId, exerciseId, 1);

        ExerciseEntity exercise = ExerciseEntity.builder().id(exerciseId).build();
        WorkoutEntity workout = workoutOwnedBy(workoutId, ownerId);
        WorkoutExerciseEntity mappedEntity = WorkoutExerciseEntity.builder().orderIndex(1).build();
        WorkoutExerciseEntity savedEntity = workoutExerciseFor(100L, workout);
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(100L, workoutId, exerciseId, 1);

        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.existsByWorkoutIdAndOrderIndex(workoutId, 1)).thenReturn(false);
        when(mapper.createRequestToEntity(request)).thenReturn(mappedEntity);
        when(workoutExerciseRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.entityToResponse(savedEntity)).thenReturn(response);

        WorkoutExerciseResponse result = service.addExerciseToWorkout(request, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
        assertThat(mappedEntity.getExercise()).isEqualTo(exercise);
        assertThat(mappedEntity.getWorkout()).isEqualTo(workout);
    }

    @Test
    @DisplayName("A user without permission cannot add an exercise to another user’s workout.")
    void addExerciseToWorkout_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutId = 10L;
        Long exerciseId = 5L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(workoutId, exerciseId, 1);

        ExerciseEntity exercise = ExerciseEntity.builder().id(exerciseId).build();
        WorkoutEntity workout = workoutOwnedBy(workoutId, ownerId);

        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> service.addExerciseToWorkout(request, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(workoutExerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adding a nonexistent exercise returns a not found error.")
    void addExerciseToWorkout_whenExerciseNotFound_throwsEntityNotFound() {
        Long exerciseId = 5L;
        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(10L, exerciseId, 1);

        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addExerciseToWorkout(request, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Adding an exercise to a nonexistent workout returns a not found error.")
    void addExerciseToWorkout_whenWorkoutNotFound_throwsEntityNotFound() {
        Long workoutId = 10L;
        Long exerciseId = 5L;
        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(workoutId, exerciseId, 1);
        ExerciseEntity exercise = ExerciseEntity.builder().id(exerciseId).build();

        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(workoutRepository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addExerciseToWorkout(request, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Adding an exercise with an occupied order index returns a conflict.")
    void addExerciseToWorkout_whenOrderIndexAlreadyUsed_throwsEntityExists() {
        Long workoutId = 10L;
        Long exerciseId = 5L;
        Long ownerId = 1L;
        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(workoutId, exerciseId, 1);

        ExerciseEntity exercise = ExerciseEntity.builder().id(exerciseId).build();
        WorkoutEntity workout = workoutOwnedBy(workoutId, ownerId);

        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.existsByWorkoutIdAndOrderIndex(workoutId, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.addExerciseToWorkout(request, ownerDetails(ownerId)))
                .isInstanceOf(EntityExistsException.class);

        verify(workoutExerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("The owner can update the exercise order successfully.")
    void updateOrderIndex_whenOwner_updatesSuccessfully() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        UpdateWorkoutExerciseRequest request = new UpdateWorkoutExerciseRequest(2);
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(workoutExerciseId, 10L, 5L, 2);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));
        when(workoutExerciseRepository.save(entity)).thenReturn(entity);
        when(mapper.entityToResponse(entity)).thenReturn(response);

        WorkoutExerciseResponse result = service.updateOrderIndex(workoutExerciseId, request, ownerDetails(ownerId));

        assertThat(result).isEqualTo(response);
        verify(mapper).updateEntityFromRequest(request, entity);
    }

    @Test
    @DisplayName("A user without permission cannot update the exercise order.")
    void updateOrderIndex_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        UpdateWorkoutExerciseRequest request = new UpdateWorkoutExerciseRequest(2);
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateOrderIndex(workoutExerciseId, request, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(workoutExerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("The owner can remove an exercise from a workout successfully.")
    void removeExerciseFromWorkout_whenOwner_deletesSuccessfully() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));

        service.removeExerciseFromWorkout(workoutExerciseId, ownerDetails(ownerId));

        verify(workoutExerciseRepository).deleteById(workoutExerciseId);
    }

    @Test
    @DisplayName("An administrator can remove an exercise from any workout.")
    void removeExerciseFromWorkout_whenAdmin_deletesAny() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        Long adminId = 999L;
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));

        service.removeExerciseFromWorkout(workoutExerciseId, adminDetails(adminId));

        verify(workoutExerciseRepository).deleteById(workoutExerciseId);
    }

    @Test
    @DisplayName("A user without permission cannot remove an exercise from another user’s workout.")
    void removeExerciseFromWorkout_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long workoutExerciseId = 100L;
        Long ownerId = 1L;
        Long strangerId = 2L;
        WorkoutEntity workout = workoutOwnedBy(10L, ownerId);
        WorkoutExerciseEntity entity = workoutExerciseFor(workoutExerciseId, workout);

        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.removeExerciseFromWorkout(workoutExerciseId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(workoutExerciseRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Removing a nonexistent workout exercise returns a not found error.")
    void removeExerciseFromWorkout_whenNotFound_throwsEntityNotFound() {
        Long workoutExerciseId = 100L;
        when(workoutExerciseRepository.findById(workoutExerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeExerciseFromWorkout(workoutExerciseId, ownerDetails(1L)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(workoutExerciseRepository, never()).deleteById(any());
    }
}
