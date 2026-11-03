package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.Exception.GlobalExceptionHandler;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.WorkoutSet.dto.CreateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.UpdateWorkoutSetRequest;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WorkoutSetControllerTest {

    @Mock
    private WorkoutSetService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        WorkoutSetController controller = new WorkoutSetController(service);

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
    @DisplayName("Get a set by ID.")
    void getWorkoutSetById_returnsOk() throws Exception {
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, 100L, 1, 10, BigDecimal.valueOf(50));
        when(service.getWorkoutSetById(eq(1000L), any())).thenReturn(response);

        mockMvc.perform(get("/api/workout-set/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repsDone").value(10));
    }

    @Test
    @DisplayName("Getting a set without permission returns 403.")
    void getWorkoutSetById_whenAccessDenied_returns403() throws Exception {
        when(service.getWorkoutSetById(eq(1000L), any()))
                .thenThrow(new AccessDeniedException("Denied"));

        mockMvc.perform(get("/api/workout-set/1000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get the sets for a workout exercise.")
    void getSetsByWorkoutExercise_returnsOk() throws Exception {
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, 100L, 1, 10, BigDecimal.valueOf(50));
        when(service.getSetsByWorkoutExercise(eq(100L), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/workout-set/workout-exercise/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workoutExerciseId").value(100L));
    }

    @Test
    @DisplayName("Adding a set with a valid request returns a successful response.")
    void addSetToExercise_validRequest_returnsOk() throws Exception {
        CreateWorkoutSetRequest request = new CreateWorkoutSetRequest(100L, 1, 10, BigDecimal.valueOf(50));
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, 100L, 1, 10, BigDecimal.valueOf(50));

        when(service.addSetToExercise(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/workout-set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repsDone").value(10));
    }

    @Test
    @DisplayName("Adding a set with negative repetitions returns 400.")
    void addSetToExercise_negativeReps_returns400() throws Exception {
        String invalidJson = """
                {
                    "workoutExerciseId": 100,
                    "position": 1,
                    "repsDone": -5,
                    "weight": 50
                }
                """;

        mockMvc.perform(post("/api/workout-set")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Adding a set with a set number of zero returns 400.")
    void addSetToExercise_zeroPosition_returns400() throws Exception {
        String invalidJson = """
                {
                    "workoutExerciseId": 100,
                    "position": 0,
                    "repsDone": 10,
                    "weight": 50
                }
                """;

        mockMvc.perform(post("/api/workout-set")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Adding a set with negative weight returns 400.")
    void addSetToExercise_negativeWeight_returns400() throws Exception {
        String invalidJson = """
                {
                    "workoutExerciseId": 100,
                    "position": 1,
                    "repsDone": 10,
                    "weight": -50
                }
                """;

        mockMvc.perform(post("/api/workout-set")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Updating a set returns a successful response.")
    void updateSet_returnsOk() throws Exception {
        UpdateWorkoutSetRequest request = new UpdateWorkoutSetRequest(2, 12, BigDecimal.valueOf(55));
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, 100L, 2, 12, BigDecimal.valueOf(55));

        when(service.updateSet(any(), eq(1000L), any())).thenReturn(response);

        mockMvc.perform(put("/api/workout-set/1000")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repsDone").value(12));
    }

    @Test
    @DisplayName("Deleting a set returns a successful response.")
    void deleteSet_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/workout-set/1000"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deleting a nonexistent set returns 404.")
    void deleteSet_whenNotFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Workout set not found"))
                .when(service).deleteSet(eq(1000L), any());

        mockMvc.perform(delete("/api/workout-set/1000"))
                .andExpect(status().isNotFound());
    }
}