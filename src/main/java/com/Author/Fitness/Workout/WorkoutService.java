package com.Author.Fitness.Workout;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.SecurityUtils;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.User.UserRepository;
import com.Author.Fitness.Workout.dto.CreateWorkoutRequest;
import com.Author.Fitness.Workout.dto.UpdateWorkoutRequest;
import com.Author.Fitness.Workout.dto.WorkoutResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WorkoutService {

    private final WorkoutMapper mapper;
    private final WorkoutRepository repository;
    private final UserRepository userRepository;

    public WorkoutResponse getWorkoutById(Long id, CustomUserDetails currentUser){
        WorkoutEntity entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find workout with id: {} during get", id);
                    return new EntityNotFoundException("Workout not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getUser().getId(), currentUser);
        return mapper.entityToResponse(entity);
    }

    public Page<WorkoutResponse> getWorkoutsByUser(Long userId, int pageNumber, int pageSize, CustomUserDetails currentUser){
        SecurityUtils.checkOwnershipOrAdmin(userId, currentUser);

        Page<WorkoutEntity> entities = repository.findAllByUserIdOrderByDateDesc(userId, PageRequest.of(pageNumber, pageSize));
        return entities.map(mapper::entityToResponse);
    }

    @Transactional
    public WorkoutResponse createWorkout(CreateWorkoutRequest request, CustomUserDetails currentUser){
        SecurityUtils.checkOwnershipOrAdmin(request.userId(), currentUser);

        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> {
                    log.error("Failed to find user with id: {} during workout creation", request.userId());
                    return new EntityNotFoundException("User not found");
                });

        WorkoutEntity entity = mapper.createRequestToEntity(request);
        entity.setUser(user);

        WorkoutEntity createdWorkout = repository.save(entity);
        log.info("Workout created with id: {}", createdWorkout.getId());
        return mapper.entityToResponse(createdWorkout);
    }

    @Transactional
    public WorkoutResponse updateWorkout(UpdateWorkoutRequest request, Long id, CustomUserDetails currentUser){
        WorkoutEntity entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find workout with id: {} during update", id);
                    return new EntityNotFoundException("Workout not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getUser().getId(), currentUser);

        mapper.updateEntityFromRequest(request, entity);
        WorkoutEntity updatedWorkout = repository.save(entity);
        log.info("Workout updated with id: {}", updatedWorkout.getId());
        return mapper.entityToResponse(updatedWorkout);
    }

    @Transactional
    public void deleteWorkout(Long id, CustomUserDetails currentUser){
        WorkoutEntity entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to find workout with id: {} during delete", id);
                    return new EntityNotFoundException("Workout not found");
                });

        SecurityUtils.checkOwnershipOrAdmin(entity.getUser().getId(), currentUser);

        repository.deleteById(id);
        log.info("Workout deleted with id: {}", id);
    }
}