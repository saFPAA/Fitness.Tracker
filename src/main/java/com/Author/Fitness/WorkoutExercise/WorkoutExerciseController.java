package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.WorkoutExercise.dto.CreateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.UpdateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.WorkoutExerciseResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@RequestMapping("/api/workout-exercise")
public class WorkoutExerciseController {

    private final WorkoutExerciseService service;

    @GetMapping("/{id}")
    public WorkoutExerciseResponse getWorkoutExerciseById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getWorkoutExerciseById(id, currentUser);
    }

    @GetMapping("/workout/{workoutId}")
    public List<WorkoutExerciseResponse> getExercisesByWorkout(
            @PathVariable("workoutId") Long workoutId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getExercisesByWorkout(workoutId, currentUser);
    }

    @PostMapping
    public WorkoutExerciseResponse addExerciseToWorkout(
            @Valid @RequestBody CreateWorkoutExerciseRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.addExerciseToWorkout(request, currentUser);
    }

    @PutMapping("/{id}")
    public WorkoutExerciseResponse updateOrderIndex(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateWorkoutExerciseRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.updateOrderIndex(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    public void removeExerciseFromWorkout(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        service.removeExerciseFromWorkout(id, currentUser);
    }
}