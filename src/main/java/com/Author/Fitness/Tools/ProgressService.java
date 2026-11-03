package com.Author.Fitness.Tools;

import com.Author.Fitness.Exercise.ExerciseEntity;
import com.Author.Fitness.Exercise.ExerciseRepository;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.SecurityUtils;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.User.UserRepository;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseEntity;
import com.Author.Fitness.WorkoutExercise.WorkoutExerciseRepository;
import com.Author.Fitness.WorkoutSet.WorkoutSetEntity;
import com.Author.Fitness.WorkoutSet.WorkoutSetMapper;
import com.Author.Fitness.WorkoutSet.WorkoutSetRepository;
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
public class ProgressService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final WorkoutSetMapper mapper;

    public List<WorkoutSetResponse> getProgressForExercise(Long userId, Long exerciseId, CustomUserDetails currentUser){

        SecurityUtils.checkOwnershipOrAdmin(userId, currentUser);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to find user with id: {} during get progress", userId);
                    return new EntityNotFoundException("User not found");
                });

        ExerciseEntity exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> {
                    log.error("Failed to find exercise with id: {} during get progress", exerciseId);
                    return new EntityNotFoundException("Exercise not found");
                });

        List<WorkoutSetEntity> sets = workoutSetRepository.findProgressForExercise(userId, exerciseId);

        return sets.stream().map(mapper::entityToResponse).toList();
    }


}
