package com.Author.Fitness.Tools;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProgressAccessTest extends AbstractIntegrationTest {

    private String createBody(long workoutExerciseId, int position, int repsDone, String weight) {
        return """
                {"workoutExerciseId":%d,"position":%d,"repsDone":%d,"weight":%s}
                """.formatted(workoutExerciseId,position,repsDone,weight);
    }

    @Test
    @DisplayName("A user can read their own progress for an exercise.")
    void userCanReadOwnProgress() throws Exception{
        Tokens tokens = register("Alex");

        long workoutId = createWorkout(
                tokens.access(),
                userId("Alex"),
                "Тренировка на грудь"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId = addExerciseToWorkout(
                tokens,
                workoutId,
                exerciseId,
                1
        );

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 2, 8, "70.00")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/progress")
                        .param("exerciseId", String.valueOf(exerciseId))
                        .param("userId", String.valueOf(userId("Alex")))
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].repsDone").value(10))
                .andExpect(jsonPath("$[0].weight").value(60.00))
                .andExpect(jsonPath("$[1].repsDone").value(8))
                .andExpect(jsonPath("$[1].weight").value(70.00));
    }

    @Test
    @DisplayName("A user cannot access another user’s progress and receives 403.")
    void userCannotReadOtherUsersProgress() throws Exception {

        Tokens owner = register("Alex");

        long workoutId = createWorkout(
                owner.access(),
                userId("Alex"),
                "Тренировка Alex"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId = addExerciseToWorkout(
                owner, workoutId, exerciseId, 1
        );

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk());

        Tokens otherUser = register("Bob");

        mockMvc.perform(get("/api/progress")
                        .param("exerciseId", String.valueOf(exerciseId))
                        .param("userId", String.valueOf(userId("Alex")))
                        .header("Authorization", bearer(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An administrator can read any user’s progress.")
    void adminCanReadAnyProgress() throws Exception {

        Tokens owner = register("Alex");

        long workoutId = createWorkout(
                owner.access(),
                userId("Alex"),
                "Тренировка Alex"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId = addExerciseToWorkout(
                owner, workoutId, exerciseId, 1
        );

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk());

        Tokens admin = registerAdmin("Admin");

        mockMvc.perform(get("/api/progress")
                        .param("exerciseId", String.valueOf(exerciseId))
                        .param("userId", String.valueOf(userId("Alex")))
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].repsDone").value(10))
                .andExpect(jsonPath("$[0].weight").value(60.00));
    }

    @Test
    @DisplayName("Progress contains sets only for the requested exercise.")
    void progressContainsOnlyRequestedExercise() throws Exception {

        Tokens owner = register("Alex");

        long workoutId = createWorkout(
                owner.access(),
                userId("Alex"),
                "Тренировка"
        );

        long benchPressId = createExercise("Жим лёжа");
        long squatId = createExercise("Приседания");

        long benchPressWorkoutExerciseId = addExerciseToWorkout(
                owner, workoutId, benchPressId, 1
        );

        long squatWorkoutExerciseId = addExerciseToWorkout(
                owner, workoutId, squatId, 2
        );

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                benchPressWorkoutExerciseId, 1, 10, "60.00"
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                squatWorkoutExerciseId, 1, 8, "80.00"
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/progress")
                        .param("exerciseId", String.valueOf(benchPressId))
                        .param("userId", String.valueOf(userId("Alex")))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].repsDone").value(10))
                .andExpect(jsonPath("$[0].weight").value(60.00));
    }

    @Test
    @DisplayName("Sets are ordered by workout date in ascending order.")
    void progressIsOrderedByWorkoutDate() throws Exception {

        Tokens owner = register("Alex");

        long exerciseId = createExercise("Жим лёжа");

        long firstWorkoutId = createWorkout(
                owner.access(),
                userId("Alex"),
                "Первая тренировка"
        );

        long secondWorkoutId = createWorkout(
                owner.access(),
                userId("Alex"),
                "Вторая тренировка"
        );

        // устанавливаю разные даты тренировок
        jdbc.update(
                "UPDATE workouts SET date = ? WHERE id = ?",
                java.sql.Date.valueOf("2026-10-20"),
                firstWorkoutId
        );

        jdbc.update(
                "UPDATE workouts SET date = ? WHERE id = ?",
                java.sql.Date.valueOf("2026-10-25"),
                secondWorkoutId
        );

        long firstWorkoutExerciseId = addExerciseToWorkout(
                owner, firstWorkoutId, exerciseId, 1
        );

        long secondWorkoutExerciseId = addExerciseToWorkout(
                owner, secondWorkoutId, exerciseId, 1
        );

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                secondWorkoutExerciseId, 1, 8, "70.00"
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                firstWorkoutExerciseId, 1, 10, "60.00"
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/progress")
                        .param("exerciseId", String.valueOf(exerciseId))
                        .param("userId", String.valueOf(userId("Alex")))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].repsDone").value(10))
                .andExpect(jsonPath("$[1].repsDone").value(8));
    }

    @Test
    @DisplayName("An unknown exercise returns 404.")
    void progressForUnknownExerciseReturns404() throws Exception {

        Tokens owner = register("Alex");

        mockMvc.perform(get("/api/progress")
                        .param("exerciseId", "999999")
                        .param("userId", String.valueOf(userId("Alex")))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("An unknown user returns 404.")
    void progressForUnknownUserReturns404() throws Exception {

        Tokens owner = registerAdmin("Alex");


        long workoutId = createWorkout(
                owner.access(),
                userId("Alex"),
                "Тренировка Alex"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId = addExerciseToWorkout(
                owner, workoutId, exerciseId, 1
        );

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/progress")
                        .param("exerciseId", String.valueOf(exerciseId))
                        .param("userId", String.valueOf(8L))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }
}
