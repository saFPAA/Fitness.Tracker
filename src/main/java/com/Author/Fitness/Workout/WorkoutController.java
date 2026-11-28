package com.Author.Fitness.Workout;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Workout.dto.CreateWorkoutRequest;
import com.Author.Fitness.Workout.dto.UpdateWorkoutRequest;
import com.Author.Fitness.Workout.dto.WorkoutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@RequestMapping("/api/workout")
@Validated
public class WorkoutController {

    private final WorkoutService service;

    @Operation(summary = "Получить тренировку", description = "Доступна владельцу тренировки и администратору.")
    @ApiResponse(responseCode = "200", description = "Тренировка найдена", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Требуется действительный access-токен", content = @Content)
    @ApiResponse(responseCode = "403", description = "Нет доступа к тренировке")
    @ApiResponse(responseCode = "404", description = "Тренировка не найдена")
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
