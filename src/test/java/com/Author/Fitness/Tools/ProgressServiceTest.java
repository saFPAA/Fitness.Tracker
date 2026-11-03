package com.Author.Fitness.Tools;

import com.Author.Fitness.Exercise.ExerciseEntity;
import com.Author.Fitness.Exercise.ExerciseRepository;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.User.UserRepository;
import com.Author.Fitness.Workout.WorkoutEntity;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseEntity;
import com.Author.Fitness.WorkoutSet.WorkoutSetEntity;
import com.Author.Fitness.WorkoutSet.WorkoutSetMapper;
import com.Author.Fitness.WorkoutSet.WorkoutSetRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private WorkoutSetRepository workoutSetRepository;

    @Mock
    private WorkoutSetMapper mapper;

    @InjectMocks
    private ProgressService service;

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

    @Test
    @DisplayName("The owner receives exercise progress.")
    void getProgressForExercise_whenOwner_returnsProgress() {
        Long userId = 1L;
        Long exerciseId = 5L;

        UserEntity user = UserEntity.builder().id(userId).build();
        ExerciseEntity exercise = ExerciseEntity.builder().id(exerciseId).build();
        WorkoutEntity workout = WorkoutEntity.builder().id(10L).user(user).build();
        WorkoutExerciseEntity workoutExercise = WorkoutExerciseEntity.builder().id(100L).workout(workout).build();
        WorkoutSetEntity set = WorkoutSetEntity.builder()
                .id(1000L).workoutExercise(workoutExercise).position(1).repsDone(10).weight(BigDecimal.valueOf(50))
                .build();
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, 100L, 1, 10, BigDecimal.valueOf(50));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(workoutSetRepository.findProgressForExercise(userId, exerciseId)).thenReturn(List.of(set));
        when(mapper.entityToResponse(set)).thenReturn(response);

        List<WorkoutSetResponse> result = service.getProgressForExercise(userId, exerciseId, ownerDetails(userId));

        assertThat(result).containsExactly(response);
    }

    @Test
    @DisplayName("An administrator receives any user’s exercise progress.")
    void getProgressForExercise_whenAdmin_returnsAnyUserProgress() {
        Long userId = 1L;
        Long exerciseId = 5L;
        Long adminId = 999L;

        UserEntity user = UserEntity.builder().id(userId).build();
        ExerciseEntity exercise = ExerciseEntity.builder().id(exerciseId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(workoutSetRepository.findProgressForExercise(userId, exerciseId)).thenReturn(List.of());

        List<WorkoutSetResponse> result = service.getProgressForExercise(userId, exerciseId, adminDetails(adminId));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("A user without permission is denied access.")
    void getProgressForExercise_whenNotOwnerAndNotAdmin_throwsAccessDenied() {
        Long userId = 1L;
        Long exerciseId = 5L;
        Long strangerId = 2L;

        assertThatThrownBy(() -> service.getProgressForExercise(userId, exerciseId, ownerDetails(strangerId)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userRepository, exerciseRepository, workoutSetRepository);
    }

    @Test
    @DisplayName("A nonexistent user returns a not found error.")
    void getProgressForExercise_whenUserNotFound_throwsEntityNotFound() {
        Long userId = 1L;
        Long exerciseId = 5L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgressForExercise(userId, exerciseId, ownerDetails(userId)))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(exerciseRepository, workoutSetRepository);
    }

    @Test
    @DisplayName("A nonexistent exercise returns a not found error.")
    void getProgressForExercise_whenExerciseNotFound_throwsEntityNotFound() {
        Long userId = 1L;
        Long exerciseId = 5L;
        UserEntity user = UserEntity.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgressForExercise(userId, exerciseId, ownerDetails(userId)))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(workoutSetRepository);
    }
}