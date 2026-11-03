package com.Author.Fitness.Workout;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CascadeDeleteTest extends AbstractIntegrationTest {

    private String createBody(long workoutExerciseId, int position, int repsDone, String weight) {
        return """
                {"workoutExerciseId":%d,"position":%d,"repsDone":%d,"weight":%s}
                """.formatted(workoutExerciseId,position,repsDone,weight);
    }

    @Test
    @DisplayName("Deleting a workout also deletes its exercises and sets.")
    void deletingWorkoutRemovesItsExercisesAndSets() throws Exception {

        Tokens owner = register("Alex");

        long workoutId = createWorkout(
                owner.access(),
                userId("Alex"),
                "Тренировка на грудь"
        );

        long exerciseId = createExercise("Жим лёжа");

        long workoutExerciseId = addExerciseToWorkout(
                owner,
                workoutId,
                exerciseId,
                1
        );

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 2, 8, "70.00")))
                .andExpect(status().isOk());

        // проверяю, что связанные данные созданы
        Integer exercisesBefore = jdbc.queryForObject(
                "SELECT COUNT(id) FROM workout_exercises WHERE workout_id = ?",
                Integer.class,
                workoutId
        );

        Integer setsBefore = jdbc.queryForObject(
                """
                SELECT COUNT(ws.id)
                FROM workout_sets ws
                JOIN workout_exercises we ON ws.workout_exercise_id = we.id
                WHERE we.workout_id = ?
                """,
                Integer.class,
                workoutId
        );

        assertThat(exercisesBefore).isEqualTo(1);
        assertThat(setsBefore).isEqualTo(2);

        mockMvc.perform(delete("/api/workout/{id}", workoutId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().is2xxSuccessful());

        Integer exercisesAfter = jdbc.queryForObject(
                "SELECT COUNT(id) FROM workout_exercises WHERE workout_id = ?",
                Integer.class,
                workoutId
        );

        assertThat(exercisesAfter).isZero();

        Integer setsAfter = jdbc.queryForObject(
                """
                SELECT COUNT(ws.id)
                FROM workout_sets ws
                JOIN workout_exercises we ON ws.workout_exercise_id = we.id
                WHERE we.workout_id = ?
                """,
                Integer.class,
                workoutId
        );

        assertThat(setsAfter).isZero();
    }

    @Test
    @DisplayName("Deleting a user also deletes all of their workouts, exercises, and sets.")
    void deleteUserDeletesAllRelatedData() throws Exception {


        Tokens alex = register("Alex");
        Tokens bob = register("Bob");

        long alexId = userId("Alex");
        long bobId = userId("Bob");


        long bench = createExercise("Жим лёжа");
        long fly = createExercise("Разводка");


        long workoutId = createWorkout(alex.access(), alexId, "Грудь");


        long benchId = addExerciseToWorkout(alex, workoutId, bench, 1);
        long flyId = addExerciseToWorkout(alex, workoutId, fly, 2);


        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(alex))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutExerciseId":%d,"position":1,"repsDone":10,"weight":60}
                                """.formatted(benchId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(alex))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutExerciseId":%d,"position":2,"repsDone":8,"weight":62.5}
                                """.formatted(benchId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(alex))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutExerciseId":%d,"position":1,"repsDone":12,"weight":20}
                                """.formatted(flyId)))
                .andExpect(status().isOk());


        long bobWorkoutId = createWorkout(bob.access(), bobId, "Тренировка Bob");
        long bobBenchId = addExerciseToWorkout(bob, bobWorkoutId, bench, 1);

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutExerciseId":%d,"position":1,"repsDone":5,"weight":100}
                                """.formatted(bobBenchId)))
                .andExpect(status().isOk());


        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workouts", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_exercises", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_sets", Integer.class)).isEqualTo(4);

        int catalogBefore = jdbc.queryForObject(
                "SELECT count(*) FROM exercises", Integer.class);


        mockMvc.perform(delete("/api/user/" + alexId)
                        .header("Authorization", bearer(alex)))
                .andExpect(status().isOk());


        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workouts", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_exercises", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_sets", Integer.class)).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                alexId
        )).isZero();


        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercises", Integer.class))
                .isEqualTo(catalogBefore);


        mockMvc.perform(get("/api/workout/" + bobWorkoutId)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isOk());


        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM workout_sets WHERE workout_exercise_id = ?",
                Integer.class,
                bobBenchId
        )).isEqualTo(1);
    }


}
