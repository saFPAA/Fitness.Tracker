package com.Author.Fitness.Workout;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Workout.dto.CreateWorkoutRequest;
import com.Author.Fitness.Workout.dto.UpdateWorkoutRequest;
import com.Author.Fitness.Workout.dto.WorkoutResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workout")
@Validated
public class WorkoutController {

    private final WorkoutService service;

    @GetMapping("/{id}")
    public WorkoutResponse getWorkoutById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getWorkoutById(id, currentUser);
    }

    @GetMapping("/user/{userId}")
    public Page<WorkoutResponse> getWorkoutsByUser(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getWorkoutsByUser(userId, pageNumber, pageSize, currentUser);
    }

    @PostMapping
    public WorkoutResponse createWorkout(
            @Valid @RequestBody CreateWorkoutRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.createWorkout(request, currentUser);
    }

    @PutMapping("/{id}")
    public WorkoutResponse updateWorkout(
            @Valid @RequestBody UpdateWorkoutRequest request,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.updateWorkout(request, id, currentUser);
    }

    @DeleteMapping("/{id}")
    public void deleteWorkout(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        service.deleteWorkout(id, currentUser);
    }
}