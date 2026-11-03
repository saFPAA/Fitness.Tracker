package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Exercise.ExerciseEntity;
import com.Author.Fitness.Exercise.ExerciseRepository;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.SecurityUtils;
import com.Author.Fitness.Workout.WorkoutEntity;
import com.Author.Fitness.Workout.WorkoutRepository;
import com.Author.Fitness.WorkoutExercise.dto.CreateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.UpdateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.WorkoutExerciseResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutExerciseService {

    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutExerciseMapper mapper;

    public WorkoutExerciseResponse getWorkoutExerciseById(Long id, CustomUserDetails currentUser){
        WorkoutExerciseEntity entity = workoutExerciseRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find workout exercise with id: {} during get", id);
                    return new EntityNotFoundException("Workout exercise not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getWorkout().getUser().getId(), currentUser);
        return mapper.entityToResponse(entity);
    }

    public List<WorkoutExerciseResponse> getExercisesByWorkout(Long workoutId, CustomUserDetails currentUser){
        WorkoutEntity workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> {
                    log.error("Failed to find workout with id: {} during get exercises by workout", workoutId);
                    return new EntityNotFoundException("Workout not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(workout.getUser().getId(), currentUser);

        List<WorkoutExerciseEntity> entities = workoutExerciseRepository.findAllByWorkoutIdOrderByOrderIndexAsc(workoutId);
        return entities.stream().map(mapper::entityToResponse).toList();
    }

    @Transactional
    public WorkoutExerciseResponse addExerciseToWorkout(CreateWorkoutExerciseRequest request, CustomUserDetails currentUser){
        ExerciseEntity exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> {
                    log.error("Failed to find exercise with id: {} during workout exercise creation", request.exerciseId());
                    return new EntityNotFoundException("Exercise not found");
                });

        WorkoutEntity workout = workoutRepository.findById(request.workoutId())
                .orElseThrow(() -> {
                    log.error("Failed to find workout with id: {} during workout exercise creation", request.workoutId());
                    return new EntityNotFoundException("Workout not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(workout.getUser().getId(), currentUser);

        if (workoutExerciseRepository.existsByWorkoutIdAndOrderIndex(request.workoutId(), request.orderIndex())) {
            log.error("Order index {} already used in workout {}", request.orderIndex(), request.workoutId());
            throw new EntityExistsException("Order index already used in this workout");
        }

        WorkoutExerciseEntity entity = mapper.createRequestToEntity(request);
        entity.setExercise(exercise);
        entity.setWorkout(workout);

        WorkoutExerciseEntity createdWorkoutExercise = workoutExerciseRepository.save(entity);
        log.info("Workout exercise created with id: {}", createdWorkoutExercise.getId());
        return mapper.entityToResponse(createdWorkoutExercise);
    }

    @Transactional
    public WorkoutExerciseResponse updateOrderIndex(Long id, UpdateWorkoutExerciseRequest request, CustomUserDetails currentUser){
        WorkoutExerciseEntity entity = workoutExerciseRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find workout exercise with id: {} during update", id);
                    return new EntityNotFoundException("Workout exercise not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getWorkout().getUser().getId(), currentUser);

        mapper.updateEntityFromRequest(request, entity);
        WorkoutExerciseEntity updatedWorkoutExercise = workoutExerciseRepository.save(entity);
        log.info("Workout exercise updated with id: {}", updatedWorkoutExercise.getId());
        return mapper.entityToResponse(updatedWorkoutExercise);
    }

    @Transactional
    public void removeExerciseFromWorkout(Long id, CustomUserDetails currentUser){
        WorkoutExerciseEntity entity = workoutExerciseRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find workout exercise with id: {} during delete", id);
                    return new EntityNotFoundException("Workout exercise not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getWorkout().getUser().getId(), currentUser);

        workoutExerciseRepository.deleteById(id);
        log.info("Workout exercise deleted with id: {}", id);
    }
}