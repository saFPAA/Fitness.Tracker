package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.SecurityUtils;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseEntity;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseRepository;
import com.Author.Fitness.WorkoutSet.dto.CreateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.UpdateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
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
public class WorkoutSetService {

    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final WorkoutSetMapper mapper;

    @Transactional
    public WorkoutSetResponse addSetToExercise(CreateWorkoutSetRequest request, CustomUserDetails currentUser){

        WorkoutExerciseEntity workoutExerciseEntity = workoutExerciseRepository
                .findById(request.workoutExerciseId())
                .orElseThrow(() -> {
                    log.error("Failed to find workout exercise with id: {} during add set to exercise", request.workoutExerciseId());
                    return new EntityNotFoundException("Workout exercise not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(workoutExerciseEntity.getWorkout().getUser().getId(), currentUser);

        WorkoutSetEntity workoutSetEntity = mapper.createRequestToEntity(request);
        workoutSetEntity.setWorkoutExercise(workoutExerciseEntity);

        WorkoutSetEntity createdWorkoutSet = workoutSetRepository.save(workoutSetEntity);
        log.info("Workout set created with id: {}", createdWorkoutSet.getId());
        return mapper.entityToResponse(createdWorkoutSet);
    }

    public WorkoutSetResponse getWorkoutSetById(Long id, CustomUserDetails currentUser){

        WorkoutSetEntity entity = workoutSetRepository.findById(id)
                .orElseThrow(() ->{
                    log.error("Failed to find workout set with id: {} during get by id", id);
                    return new EntityNotFoundException("Workout set not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getWorkoutExercise().getWorkout().getUser().getId(), currentUser);
        return mapper.entityToResponse(entity);
    }

    public List<WorkoutSetResponse> getSetsByWorkoutExercise(Long workoutExerciseId, CustomUserDetails currentUser){
        WorkoutExerciseEntity workoutExercise = workoutExerciseRepository.findById(workoutExerciseId)
                .orElseThrow(() -> {
                    log.error("Failed to find workout exercise with id: {} during get sets by exercise id", workoutExerciseId);
                    return new EntityNotFoundException("Workout exercise not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(workoutExercise.getWorkout().getUser().getId(), currentUser);

        List<WorkoutSetEntity> entities = workoutSetRepository.findAllByWorkoutExerciseIdOrderByPositionAsc(workoutExerciseId);
        return entities.stream().map(mapper::entityToResponse).toList();
    }

    @Transactional
    public WorkoutSetResponse updateSet(UpdateWorkoutSetRequest request, Long id, CustomUserDetails currentUser){
        WorkoutSetEntity entity = workoutSetRepository.findById(id)
                .orElseThrow(() ->{
                    log.error("Failed to find workout set with id: {} during update set", id);
                    return new EntityNotFoundException("Workout set not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getWorkoutExercise().getWorkout().getUser().getId(), currentUser);

        mapper.updateEntityFromRequest(request,entity);
        WorkoutSetEntity updatedWorkoutSet = workoutSetRepository.save(entity);
        log.info("Workout set updated with id: {}", id);
        return mapper.entityToResponse(updatedWorkoutSet);
    }

    @Transactional
    public void deleteSet(Long id, CustomUserDetails currentUser){
        WorkoutSetEntity entity = workoutSetRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find workout set with id: {} during delete set", id);
                    return new EntityNotFoundException("Workout set not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getWorkoutExercise().getWorkout().getUser().getId(), currentUser);

        workoutSetRepository.deleteById(id);
        log.info("Workout set deleted with id: {}", id);
    }
}