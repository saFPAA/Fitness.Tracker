package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WorkoutExerciseOrderTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("A duplicate order index is rejected with 409.")
    void duplicateOrderIndexIsRejected() throws Exception{


        Tokens token = register("Alex");

        long workoutId = createWorkout(
                token.access(),
                userId("Alex"),
                "Тренировка на грудь"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId,
                1
        );

        mockMvc.perform(post("/api/workout-exercise")
                .header("Authorization",bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "workoutId":%d,
                        "exerciseId":%d,
                        "orderIndex":1
                        }
                        """.formatted(workoutId,exerciseId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order index already used in this workout"));
    }

    @Test
    @DisplayName("An order index can be changed to an available one.")
    void orderIndexCanBeUpdated() throws Exception{
        Tokens token = register("Alex");

        long workoutId = createWorkout(
                token.access(),
                userId("Alex"),
                "Тренировка на грудь"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId,
                1
        );



        mockMvc.perform(put("/api/workout-exercise/"+workoutExerciseId)
                .header("Authorization",bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "orderIndex":2
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderIndex").value(2));
    }

    @Test
    @DisplayName("Changing to an occupied order index is rejected.")
    void updatingToOccupiedOrderIndexIsRejected() throws Exception{
        Tokens token = register("Alex");

        long workoutId = createWorkout(
                token.access(),
                userId("Alex"),
                "Тренировка на грудь"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId1 = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId,
                1
        );
        long workoutExerciseId2 = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId,
                2
        );

        mockMvc.perform(put("/api/workout-exercise/"+workoutExerciseId1)
                        .header("Authorization",bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                        "orderIndex":2
                        }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Exercises are returned in ascending order.")
    void exercisesAreReturnedInOrder() throws Exception{
        Tokens token = register("Alex");

        long workoutId = createWorkout(
                token.access(),
                userId("Alex"),
                "Тренировка на грудь"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId1 = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId,
                2
        );
        long workoutExerciseId2 = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId,
                1
        );

        mockMvc.perform(get("/api/workout-exercise/workout/"+workoutId)
                .header("Authorization",bearer(token)))
                .andExpect(jsonPath("$.[0].orderIndex").value(1))
                .andExpect(jsonPath("$.[1].orderIndex").value(2));
    }

    @Test
    @DisplayName("Two exercises can be swapped using a temporary index.")
    void twoExercisesCanBeSwappedViaTemporaryIndex() throws Exception {
        Tokens token = register("Alex");

        long workoutId = createWorkout(
                token.access(),
                userId("Alex"),
                "Тренировка на грудь"
        );

        long exerciseId1 = createExercise("Жим лёжа");
        long exerciseId2 = createExercise("Разводка гантелей");

        long workoutExerciseId1 = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId1,
                1
        );

        long workoutExerciseId2 = addExerciseToWorkout(
                token,
                workoutId,
                exerciseId2,
                2
        );

        // временный номер.
        mockMvc.perform(put("/api/workout-exercise/" + workoutExerciseId1)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "orderIndex": 999
                            }
                            """))
                .andExpect(status().isOk());

        // второе упражнение перемещаю на освободившийся
        mockMvc.perform(put("/api/workout-exercise/" + workoutExerciseId2)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "orderIndex": 1
                            }
                            """))
                .andExpect(status().isOk());

        // первое упражнение перемещаю на номер 2.
        mockMvc.perform(put("/api/workout-exercise/" + workoutExerciseId1)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "orderIndex": 2
                            }
                            """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/workout-exercise/workout/" + workoutId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exerciseId").value(exerciseId2))
                .andExpect(jsonPath("$[0].orderIndex").value(1))
                .andExpect(jsonPath("$[1].exerciseId").value(exerciseId1))
                .andExpect(jsonPath("$[1].orderIndex").value(2));
    }


}
