package com.Author.Fitness.Exercise;

import com.Author.Fitness.Exercise.dto.CreateExerciseRequest;
import com.Author.Fitness.Exercise.dto.ExerciseResponse;
import com.Author.Fitness.Exercise.dto.UpdateExerciseRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@RequestMapping("/api/exercise")
public class ExerciseController {

    private final ExerciseService service;

    @GetMapping("/{id}")
    public ExerciseResponse getExerciseById(@PathVariable("id") Long id){
        return service.getExerciseById(id);
    }

    @GetMapping
    public Page<ExerciseResponse> getAllExercises(
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize
    ){
        return service.getAllExercises(pageNumber, pageSize);
    }

    @GetMapping("/muscle-group/{muscleGroup}")
    public Page<ExerciseResponse> getExercisesByMuscleGroup(
            @PathVariable("muscleGroup") String muscleGroup,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize
    ){
        return service.getExercisesByMuscleGroup(pageNumber, pageSize, muscleGroup);
    }

    @GetMapping("/search")
    public Page<ExerciseResponse> searchExercisesByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize
    ){
        return service.searchExercisesByName(pageNumber, pageSize, name);
    }

    @PostMapping
    public ExerciseResponse createExercise(@Valid @RequestBody CreateExerciseRequest request){
        return service.createExercise(request);
    }

    @PutMapping("/{id}")
    public ExerciseResponse updateExercise(
            @Valid @RequestBody UpdateExerciseRequest request,
            @PathVariable("id") Long id
    ){
        return service.updateExercise(request, id);
    }

    @DeleteMapping("/{id}")
    public void deleteExercise(@PathVariable("id") Long id){
        service.deleteExercise(id);
    }
}