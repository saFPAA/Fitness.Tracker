package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Exception.GlobalExceptionHandler;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.WorkoutExercise.dto.CreateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.UpdateWorkoutExerciseRequest;
import com.Author.Fitness.WorkoutExercise.dto.WorkoutExerciseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WorkoutExerciseControllerTest {

    @Mock
    private WorkoutExerciseService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        WorkoutExerciseController controller = new WorkoutExerciseController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();

        UserEntity currentUserEntity = UserEntity.builder()
                .id(1L)
                .username("current")
                .password("hashed")
                .role(Role.USER)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(currentUserEntity);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Get a workout exercise by ID.")
    void getWorkoutExerciseById_returnsOk() throws Exception {
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(100L, 10L, 5L, 1);
        when(service.getWorkoutExerciseById(eq(100L), any())).thenReturn(response);

        mockMvc.perform(get("/api/workout-exercise/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderIndex").value(1));
    }

    @Test
    @DisplayName("Getting a workout exercise without permission returns 403.")
    void getWorkoutExerciseById_whenAccessDenied_returns403() throws Exception {
        when(service.getWorkoutExerciseById(eq(100L), any()))
                .thenThrow(new AccessDeniedException("Denied"));

        mockMvc.perform(get("/api/workout-exercise/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get the exercises in a workout.")
    void getExercisesByWorkout_returnsOk() throws Exception {
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(100L, 10L, 5L, 1);
        when(service.getExercisesByWorkout(eq(10L), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/workout-exercise/workout/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workoutId").value(10L));
    }

    @Test
    @DisplayName("Adding an exercise with a valid request returns a successful response.")
    void addExerciseToWorkout_validRequest_returnsOk() throws Exception {
        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(10L, 5L, 1);
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(100L, 10L, 5L, 1);

        when(service.addExerciseToWorkout(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/workout-exercise")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId").value(5L));
    }

    @Test
    @DisplayName("Adding an exercise without a workout ID returns 400.")
    void addExerciseToWorkout_missingWorkoutId_returns400() throws Exception {
        String invalidJson = """
                {
                    "exerciseId": 5,
                    "orderIndex": 1
                }
                """;

        mockMvc.perform(post("/api/workout-exercise")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Adding an exercise with an occupied order index returns 409.")
    void addExerciseToWorkout_whenOrderIndexTaken_returns409() throws Exception {
        CreateWorkoutExerciseRequest request = new CreateWorkoutExerciseRequest(10L, 5L, 1);

        when(service.addExerciseToWorkout(any(), any()))
                .thenThrow(new EntityExistsException("Order index already used in this workout"));

        mockMvc.perform(post("/api/workout-exercise")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Updating the exercise order returns a successful response.")
    void updateOrderIndex_returnsOk() throws Exception {
        UpdateWorkoutExerciseRequest request = new UpdateWorkoutExerciseRequest(2);
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(100L, 10L, 5L, 2);

        when(service.updateOrderIndex(eq(100L), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/workout-exercise/100")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderIndex").value(2));
    }

    @Test
    @DisplayName("Updating the order without an order index returns 400.")
    void updateOrderIndex_missingOrderIndex_returns400() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(put("/api/workout-exercise/100")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Removing an exercise from a workout returns a successful response.")
    void removeExerciseFromWorkout_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/workout-exercise/100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Removing a nonexistent workout exercise returns 404.")
    void removeExerciseFromWorkout_whenNotFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Workout exercise not found"))
                .when(service).removeExerciseFromWorkout(eq(100L), any());

        mockMvc.perform(delete("/api/workout-exercise/100"))
                .andExpect(status().isNotFound());
    }
}