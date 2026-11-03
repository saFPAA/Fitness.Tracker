package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.WorkoutSet.dto.CreateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.UpdateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workout-set")
public class WorkoutSetController {

    private final WorkoutSetService service;

    @GetMapping("/{id}")
    public WorkoutSetResponse getWorkoutSetById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getWorkoutSetById(id, currentUser);
    }

    @GetMapping("/workout-exercise/{workoutExerciseId}")
    public List<WorkoutSetResponse> getSetsByWorkoutExercise(
            @PathVariable("workoutExerciseId") Long workoutExerciseId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getSetsByWorkoutExercise(workoutExerciseId, currentUser);
    }

    @PostMapping
    public WorkoutSetResponse addSetToExercise(
            @Valid @RequestBody CreateWorkoutSetRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.addSetToExercise(request, currentUser);
    }

    @PutMapping("/{id}")
    public WorkoutSetResponse updateSet(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateWorkoutSetRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.updateSet(request, id, currentUser);
    }

    @DeleteMapping("/{id}")
    public void deleteSet(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        service.deleteSet(id, currentUser);
    }
}