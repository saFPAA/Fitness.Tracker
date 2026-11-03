package com.Author.Fitness.Tools;

import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService service;

    @GetMapping
    public List<WorkoutSetResponse> getProgressForExercise(
            @RequestParam Long userId,
            @RequestParam Long exerciseId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getProgressForExercise(userId, exerciseId, currentUser);
    }
}