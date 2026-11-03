package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WorkoutSetAccessTest extends AbstractIntegrationTest {

    private String createBody(long workoutExerciseId, int position, int repsDone, String weight) {
        return """
                {"workoutExerciseId":%d,"position":%d,"repsDone":%d,"weight":%s}
                """.formatted(workoutExerciseId,position,repsDone,weight);
    }

    @Test
    @DisplayName("The owner can add a set to their exercise.")
    void ownerCanAddSet() throws Exception{

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

        mockMvc.perform(post("/api/workout-set")
                .header("Authorization",bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(workoutExerciseId,1,10,"60.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.repsDone").value(10))
                .andExpect(jsonPath("$.weight").value(60.00));

        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(id)
                FROM workout_sets
                WHERE workout_exercise_id = ?
                """,
                Integer.class,
                workoutExerciseId
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("A user cannot add a set to another user’s exercise and receives 403.")
    void userCannotAddSetToOtherUsersExercise() throws Exception {

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

        Tokens otherUser = register("Bob");

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isForbidden());

        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(id)
                FROM workout_sets
                WHERE workout_exercise_id = ?
                """,
                Integer.class,
                workoutExerciseId
        );

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("A user cannot read another user’s set and receives 403.")
    void userCannotReadOtherUsersSet() throws Exception {

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

        String response = mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = JsonPath.read(response, "$.id");
        long setId = id.longValue();

        Tokens otherUser = register("Bob");

        mockMvc.perform(get("/api/workout-set/{id}", setId)
                        .header("Authorization", bearer(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot list sets for another user’s exercise and receives 403.")
    void userCannotListOtherUsersSets() throws Exception {

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

        mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk());

        Tokens otherUser = register("Bob");

        mockMvc.perform(get(
                        "/api/workout-set/workout-exercise/{workoutExerciseId}",
                        workoutExerciseId
                )
                        .header("Authorization", bearer(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot update another user’s set and receives 403.")
    void userCannotUpdateOtherUsersSet() throws Exception{
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

        String response = mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long setId = ((Number) JsonPath.read(response, "$.id")).longValue();

        Tokens otherUser = register("Bob");

        mockMvc.perform(put("/api/workout-set/{id}", setId)
                        .header("Authorization", bearer(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "position": 1,
                                "repsDone": 8,
                                "weight": 60.00
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot delete another user’s set and receives 403.")
    void userCannotDeleteOtherUsersSet() throws Exception {

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

        String response = mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long setId = ((Number) JsonPath.read(response, "$.id")).longValue();

        Tokens otherUser = register("Bob");

        mockMvc.perform(delete("/api/workout-set/{id}", setId)
                        .header("Authorization", bearer(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An administrator can manage any set.")
    void adminCanManageAnySet() throws Exception {

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

        Tokens admin = registerAdmin("Admin");

        String response = mockMvc.perform(post("/api/workout-set")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(workoutExerciseId, 1, 10, "60.00")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long setId = ((Number) JsonPath.read(response, "$.id")).longValue();


        mockMvc.perform(get("/api/workout-set/{id}", setId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());


        mockMvc.perform(put("/api/workout-set/{id}", setId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "position": 2,
                                "repsDone": 8,
                                "weight": 70.00
                            }
                            """))
                .andExpect(status().is2xxSuccessful());


        mockMvc.perform(delete("/api/workout-set/{id}", setId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Negative repetitions are rejected with 400.")
    void negativeRepsAreRejected() throws Exception {

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
                        .content(createBody(workoutExerciseId, 1, -1, "60.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Negative weight is rejected with 400.")
    void negativeWeightIsRejected() throws Exception {

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
                        .content(createBody(workoutExerciseId, 1, 10, "-5.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A set number of zero or less is rejected with 400.")
    void zeroSetNumberIsRejected() throws Exception {

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
                        .content(createBody(workoutExerciseId, 0, 10, "60.00")))
                .andExpect(status().isBadRequest());
    }



}
