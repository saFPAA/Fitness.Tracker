package com.Author.Fitness.Exercise;

import com.Author.Fitness.Exception.GlobalExceptionHandler;
import com.Author.Fitness.Exercise.dto.CreateExerciseRequest;
import com.Author.Fitness.Exercise.dto.ExerciseResponse;
import com.Author.Fitness.Exercise.dto.UpdateExerciseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class ExerciseControllerTest {

    @Mock
    private ExerciseService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ExerciseController controller = new ExerciseController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @Test
    @DisplayName("Get exercise by ID.")
    void getExerciseById_returnsOk() throws Exception {
        ExerciseResponse response = new ExerciseResponse(1L, "Bench press", "Chest", "Barbell", null, null, null);
        when(service.getExerciseById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/exercise/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bench press"));
    }

    @Test
    @DisplayName("Getting a nonexistent exercise returns 404")
    void getExerciseById_whenNotFound_returns404() throws Exception {
        when(service.getExerciseById(1L)).thenThrow(new EntityNotFoundException("Exercise not found"));

        mockMvc.perform(get("/api/exercise/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get the exercise list")
    void getAllExercises_returnsOk() throws Exception {
        ExerciseResponse response = new ExerciseResponse(1L, "Bench press", "Chest", "Barbell", null, null, null);
        Page<ExerciseResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(service.getAllExercises(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/exercise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bench press"));
    }

    @Test
    @DisplayName("Invalid page number returns 400")
    void getAllExercises_invalidPageNumber_returns400() throws Exception {
        mockMvc.perform(get("/api/exercise").param("pageNumber", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Page size above the limit returns 400")
    void getAllExercises_pageSizeTooLarge_returns400() throws Exception {
        mockMvc.perform(get("/api/exercise").param("pageSize", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Filter exercises by muscle group")
    void getExercisesByMuscleGroup_returnsOk() throws Exception {
        ExerciseResponse response = new ExerciseResponse(1L, "Bench press", "Chest", "Barbell", null, null, null);
        Page<ExerciseResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(service.getExercisesByMuscleGroup(0, 10, "Chest")).thenReturn(page);

        mockMvc.perform(get("/api/exercise/muscle-group/Chest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].muscleGroup").value("Chest"));
    }

    @Test
    @DisplayName("Search exercises by name")
    void searchExercisesByName_returnsOk() throws Exception {
        ExerciseResponse response = new ExerciseResponse(1L, "Bench press", "Chest", "Barbell", null, null, null);
        Page<ExerciseResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(service.searchExercisesByName(0, 10, "bench")).thenReturn(page);

        mockMvc.perform(get("/api/exercise/search").param("name", "bench"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bench press"));
    }

    @Test
    @DisplayName("Create an exercise")
    void createExercise_validRequest_returnsOk() throws Exception {
        CreateExerciseRequest request = new CreateExerciseRequest("Bench press", "Chest", "Barbell", null, null, null);
        ExerciseResponse response = new ExerciseResponse(1L, "Bench press", "Chest", "Barbell", null, null, null);

        when(service.createExercise(any())).thenReturn(response);

        mockMvc.perform(post("/api/exercise")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bench press"));
    }

    @Test
    @DisplayName("Creating an exercise without a name returns 400")
    void createExercise_missingName_returns400() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "muscleGroup": "Chest"
                }
                """;

        mockMvc.perform(post("/api/exercise")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Creating an exercise without a muscle group returns 400")
    void createExercise_missingMuscleGroup_returns400() throws Exception {
        String invalidJson = """
                {
                    "name": "Bench press",
                    "muscleGroup": ""
                }
                """;

        mockMvc.perform(post("/api/exercise")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update an exercise")
    void updateExercise_returnsOk() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline bench press", "Chest", "Barbell", null, null, null);
        ExerciseResponse response = new ExerciseResponse(1L, "Incline bench press", "Chest", "Barbell", null, null, null);

        when(service.updateExercise(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(put("/api/exercise/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Incline bench press"));
    }

    @Test
    @DisplayName("Delete an exercise")
    void deleteExercise_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/exercise/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deleting an exercise used in workouts returns 409")
    void deleteExercise_whenUsedInWorkouts_returns409() throws Exception {
        doThrow(new EntityExistsException("Cannot delete exercise: it is used in existing workouts"))
                .when(service).deleteExercise(1L);

        mockMvc.perform(delete("/api/exercise/1"))
                .andExpect(status().isConflict());
    }
}