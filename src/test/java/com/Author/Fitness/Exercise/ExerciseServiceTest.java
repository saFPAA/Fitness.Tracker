package com.Author.Fitness.Exercise;

import com.Author.Fitness.Exercise.dto.CreateExerciseRequest;
import com.Author.Fitness.Exercise.dto.ExerciseResponse;
import com.Author.Fitness.Exercise.dto.UpdateExerciseRequest;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseRepository;
import jakarta.persistence.EntityExistsException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseMapper mapper;

    @Mock
    private ExerciseRepository repository;

    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;

    @InjectMocks
    private ExerciseService service;


    @Test
    @DisplayName("Get an existing exercise by ID")
    void getExerciseById_whenFound_returnsExercise() {
        Long id = 1L;
        ExerciseEntity entity = ExerciseEntity.builder().id(id).name("Bench press").build();
        ExerciseResponse response = new ExerciseResponse(id, "Bench press", "Chest", null, null, null, null);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponse(entity)).thenReturn(response);

        ExerciseResponse result = service.getExerciseById(id);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Getting a nonexistent exercise returns a not found error")
    void getExerciseById_whenNotFound_throwsEntityNotFound() {
        Long id = 1L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExerciseById(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Get exercises as a page")
    void getAllExercises_returnsPage() {
        ExerciseEntity entity = ExerciseEntity.builder().id(1L).name("Bench press").build();
        ExerciseResponse response = new ExerciseResponse(1L, "Bench press", "Chest", null, null, null, null);
        Page<ExerciseEntity> page = new PageImpl<>(java.util.List.of(entity), PageRequest.of(0, 10), 1);

        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(mapper.entityToResponse(entity)).thenReturn(response);

        Page<ExerciseResponse> result = service.getAllExercises(0, 10);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("Create an exercise successfully")
    void createExercise_success() {
        CreateExerciseRequest request = new CreateExerciseRequest("Bench press", "Chest", "Barbell", null, null, null);
        ExerciseEntity mappedEntity = ExerciseEntity.builder().name("Bench press").muscleGroup("Chest").build();
        ExerciseEntity savedEntity = ExerciseEntity.builder().id(1L).name("Bench press").muscleGroup("Chest").build();
        ExerciseResponse response = new ExerciseResponse(1L, "Bench press", "Chest", "Barbell", null, null, null);

        when(mapper.createRequestToEntity(request)).thenReturn(mappedEntity);
        when(repository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.entityToResponse(savedEntity)).thenReturn(response);

        ExerciseResponse result = service.createExercise(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Update an existing exercise successfully")
    void updateExercise_whenFound_updatesSuccessfully() {
        Long id = 1L;
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline bench press", "Chest", "Barbell", null, null, null);
        ExerciseEntity entity = ExerciseEntity.builder().id(id).name("Bench press").build();
        ExerciseResponse response = new ExerciseResponse(id, "Incline bench press", "Chest", "Barbell", null, null, null);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.entityToResponse(entity)).thenReturn(response);

        ExerciseResponse result = service.updateExercise(request, id);

        assertThat(result).isEqualTo(response);
        verify(mapper).updateEntityFromRequest(request, entity);
    }

    @Test
    @DisplayName("Updating a nonexistent exercise returns a not found error")
    void updateExercise_whenNotFound_throwsEntityNotFound() {
        Long id = 1L;
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline bench press", "Chest", "Barbell", null, null, null);

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateExercise(request, id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Delete an exercise that is not used in workouts")
    void deleteExercise_whenNotUsedInWorkouts_deletesSuccessfully() {
        Long id = 1L;
        when(repository.existsById(id)).thenReturn(true);
        when(workoutExerciseRepository.existsByExerciseId(id)).thenReturn(false);

        service.deleteExercise(id);

        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("Deleting a nonexistent exercise returns a not found error")
    void deleteExercise_whenNotFound_throwsEntityNotFound() {
        Long id = 1L;
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteExercise(id))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).deleteById(any());
        verifyNoInteractions(workoutExerciseRepository);
    }

    @Test
    @DisplayName("Deleting an exercise used in workouts returns a conflict")
    void deleteExercise_whenUsedInWorkouts_throwsEntityExists() {
        Long id = 1L;
        when(repository.existsById(id)).thenReturn(true);
        when(workoutExerciseRepository.existsByExerciseId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteExercise(id))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("used in existing workouts");

        verify(repository, never()).deleteById(any());
    }
}