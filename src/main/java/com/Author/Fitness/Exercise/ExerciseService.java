package com.Author.Fitness.Exercise;

import com.Author.Fitness.Exercise.dto.CreateExerciseRequest;
import com.Author.Fitness.Exercise.dto.ExerciseResponse;
import com.Author.Fitness.Exercise.dto.UpdateExerciseRequest;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseMapper mapper;
    private final ExerciseRepository repository;
    private final WorkoutExerciseRepository workoutExerciseRepository;

    public ExerciseResponse getExerciseById(Long id){
        ExerciseEntity entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find exercise with id: {} during get", id);
                    return new EntityNotFoundException("Exercise not found");
                });

        return mapper.entityToResponse(entity);
    }

    public Page<ExerciseResponse> getAllExercises(int pageNumber, int pageSize){
        Page<ExerciseEntity> entities = repository.findAll(PageRequest.of(pageNumber,pageSize));
        return entities.map(mapper::entityToResponse);
    }

    public Page<ExerciseResponse> getExercisesByMuscleGroup(int pageNumber,int pageSize, String muscleGroup){
        Page<ExerciseEntity> entities = repository.findAllByMuscleGroup(muscleGroup, PageRequest.of(pageNumber,pageSize));
        return entities.map(mapper::entityToResponse);
    }

    public Page<ExerciseResponse> searchExercisesByName(int pageNumber, int pageSize, String name){
        Page<ExerciseEntity> entities = repository.findByNameContainingIgnoreCase(name, PageRequest.of(pageNumber,pageSize));
        return entities.map(mapper::entityToResponse);
    }

    @Transactional
    public ExerciseResponse createExercise(CreateExerciseRequest request){

        ExerciseEntity entity = mapper.createRequestToEntity(request);

        ExerciseEntity createdExercise = repository.save(entity);
        log.info("Exercise created with id: {}", createdExercise.getId());
        return mapper.entityToResponse(createdExercise);
    }

    @Transactional
    public ExerciseResponse updateExercise(UpdateExerciseRequest request, Long id){
        ExerciseEntity entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find exercise with id: {} during update", id);
                    return new EntityNotFoundException("Exercise not found");
                });

        mapper.updateEntityFromRequest(request,entity);

        ExerciseEntity updatedExercise = repository.save(entity);
        log.info("Exercise updated with id: {}", updatedExercise.getId());
        return mapper.entityToResponse(updatedExercise);
    }

    @Transactional
    public void deleteExercise(Long id){
        if (!repository.existsById(id)) {
            log.error("Failed to find exercise with id: {} during delete", id);
            throw new EntityNotFoundException("Exercise not found");
        }

        if (workoutExerciseRepository.existsByExerciseId(id)) {
            log.error("Cannot delete exercise with id: {} — it is used in existing workouts", id);
            throw new EntityExistsException("Cannot delete exercise: it is used in existing workouts");
        }

        repository.deleteById(id);
        log.info("Exercise deleted with id: {}", id);
    }
}