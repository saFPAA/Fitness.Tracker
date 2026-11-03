package com.Author.Fitness.Workout;

import com.Author.Fitness.Exception.GlobalExceptionHandler;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.Workout.dto.CreateWorkoutRequest;
import com.Author.Fitness.Workout.dto.UpdateWorkoutRequest;
import com.Author.Fitness.Workout.dto.WorkoutResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WorkoutControllerTest {

    @Mock
    private WorkoutService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        WorkoutController controller = new WorkoutController(service);

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
    @DisplayName("Get a workout by ID.")
    void getWorkoutById_returnsOk() throws Exception {
        WorkoutResponse response = new WorkoutResponse(10L, "Push day", LocalDate.now(), 1L);
        when(service.getWorkoutById(eq(10L), any())).thenReturn(response);

        mockMvc.perform(get("/api/workout/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Push day"));
    }

    @Test
    @DisplayName("Getting a workout without permission returns 403.")
    void getWorkoutById_whenAccessDenied_returns403() throws Exception {
        when(service.getWorkoutById(eq(10L), any()))
                .thenThrow(new AccessDeniedException("Denied"));

        mockMvc.perform(get("/api/workout/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Getting a nonexistent workout returns 404.")
    void getWorkoutById_whenNotFound_returns404() throws Exception {
        when(service.getWorkoutById(eq(10L), any()))
                .thenThrow(new EntityNotFoundException("Workout not found"));

        mockMvc.perform(get("/api/workout/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get a user’s workout list.")
    void getWorkoutsByUser_returnsOk() throws Exception {
        WorkoutResponse response = new WorkoutResponse(10L, "Push day", LocalDate.now(), 1L);
        Page<WorkoutResponse> page = new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 10),
                1
        );

        when(service.getWorkoutsByUser(eq(1L), eq(0), eq(10), any())).thenReturn(page);

        mockMvc.perform(get("/api/workout/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Push day"));
    }

    @Test
    @DisplayName("Creating a workout with a valid request returns a successful response.")
    void createWorkout_validRequest_returnsOk() throws Exception {
        CreateWorkoutRequest request = new CreateWorkoutRequest("Push day", LocalDate.now(), 1L);
        WorkoutResponse response = new WorkoutResponse(10L, "Push day", LocalDate.now(), 1L);

        when(service.createWorkout(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/workout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Push day"));
    }

    @Test
    @DisplayName("Creating a workout without a name returns 400.")
    void createWorkout_missingName_returns400() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "date": "%s",
                    "userId": 1
                }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/workout")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Creating a workout without a date returns 400.")
    void createWorkout_missingDate_returns400() throws Exception {
        String invalidJson = """
                {
                    "name": "Push day",
                    "userId": 1
                }
                """;

        mockMvc.perform(post("/api/workout")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Updating a workout returns a successful response.")
    void updateWorkout_returnsOk() throws Exception {
        UpdateWorkoutRequest request = new UpdateWorkoutRequest("Updated name", LocalDate.now());
        WorkoutResponse response = new WorkoutResponse(10L, "Updated name", LocalDate.now(), 1L);

        when(service.updateWorkout(any(), eq(10L), any())).thenReturn(response);

        mockMvc.perform(put("/api/workout/10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated name"));
    }

    @Test
    @DisplayName("Deleting a workout returns a successful response.")
    void deleteWorkout_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/workout/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deleting a workout without permission returns 403.")
    void deleteWorkout_whenAccessDenied_returns403() throws Exception {
        doThrow(new AccessDeniedException("Denied"))
                .when(service).deleteWorkout(eq(10L), any());

        mockMvc.perform(delete("/api/workout/10"))
                .andExpect(status().isForbidden());
    }
}