package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.WorkoutSet.dto.CreateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.UpdateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Добавить подход",
            description = "С тем же requestId и теми же данными возвращает исходный результат без создания нового подхода."
    )
    @ApiResponse(responseCode = "200", description = "Подход создан или возвращён результат первого запроса",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Этот requestId уже использован с другими данными")
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