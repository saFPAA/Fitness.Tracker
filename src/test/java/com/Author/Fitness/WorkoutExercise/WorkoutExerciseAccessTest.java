package com.Author.Fitness.WorkoutExercise;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkoutExerciseAccessTest extends AbstractIntegrationTest {

    private String createBody(long workoutId, long exerciseId, int orderIndex) {
        return """
                {"workoutId":%d,"exerciseId":%d,"orderIndex":%d}
                """.formatted(workoutId, exerciseId, orderIndex);
    }

    @Test
    @DisplayName("The owner can add an exercise to their workout.")
    void ownerCanAddExerciseToOwnWorkout() throws Exception {
        Tokens alex = register("Alex");
        long workoutId = createWorkout(alex.access(), userId("Alex"), "Грудь");
        long exerciseId = createExercise("Жим лёжа");

        mockMvc.perform(post("/api/workout-exercise")
                        .header("Authorization", bearer(alex))
                        .contentType(APPLICATION_JSON)
                        .content(createBody(workoutId, exerciseId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutId").value(workoutId))
                .andExpect(jsonPath("$.exerciseId").value(exerciseId))
                .andExpect(jsonPath("$.orderIndex").value(1));

        assertThat(countWorkoutExercises(workoutId)).isEqualTo(1);
    }

    @Test
    @DisplayName("A user cannot add an exercise to another user’s workout and receives 403.")
    void userCannotAddExerciseToOtherUsersWorkout() throws Exception {
        Tokens alex = register("Alex");
        Tokens bob = register("Bob");
        long workoutId = createWorkout(alex.access(), userId("Alex"), "Тренировка Alex");
        long exerciseId = createExercise("Жим лёжа");

        mockMvc.perform(post("/api/workout-exercise")
                        .header("Authorization", bearer(bob))
                        .contentType(APPLICATION_JSON)
                        .content(createBody(workoutId, exerciseId, 1)))
                .andExpect(status().isForbidden());

        assertThat(countWorkoutExercises(workoutId)).isZero();
    }

    @Test
    @DisplayName("A user cannot read an exercise in another user’s workout and receives 403.")
    void userCannotReadOtherUsersWorkoutExercise() throws Exception {
        Tokens alex = register("Alex");
        Tokens bob = register("Bob");
        long workoutId = createWorkout(alex.access(), userId("Alex"), "Тренировка Alex");
        long id = addExerciseToWorkout(alex, workoutId, createExercise("Жим лёжа"), 1);

        mockMvc.perform(get("/api/workout-exercise/" + id)
                        .header("Authorization", bearer(alex)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/workout-exercise/" + id)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot list exercises in another user’s workout and receives 403.")
    void userCannotListExercisesOfOtherUsersWorkout() throws Exception {
        Tokens alex = register("Alex");
        Tokens bob = register("Bob");
        long workoutId = createWorkout(alex.access(), userId("Alex"), "Тренировка Alex");
        addExerciseToWorkout(alex, workoutId, createExercise("Жим лёжа"), 1);

        mockMvc.perform(get("/api/workout-exercise/workout/" + workoutId)
                        .header("Authorization", bearer(alex)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/workout-exercise/workout/" + workoutId)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot reorder exercises in another user’s workout and receives 403.")
    void userCannotReorderOtherUsersWorkoutExercise() throws Exception {
        Tokens alex = register("Alex");
        Tokens bob = register("Bob");
        long workoutId = createWorkout(alex.access(), userId("Alex"), "Тренировка Alex");
        long id = addExerciseToWorkout(alex, workoutId, createExercise("Жим лёжа"), 1);

        mockMvc.perform(put("/api/workout-exercise/" + id)
                        .header("Authorization", bearer(bob))
                        .contentType(APPLICATION_JSON)
                        .content("{\"orderIndex\":5}"))
                .andExpect(status().isForbidden());

        Integer orderIndex = jdbc.queryForObject(
                "SELECT order_index FROM workout_exercises WHERE id = ?", Integer.class, id);
        assertThat(orderIndex).isEqualTo(1);
    }

    @Test
    @DisplayName("A user cannot remove an exercise from another user’s workout and receives 403.")
    void userCannotRemoveExerciseFromOtherUsersWorkout() throws Exception {
        Tokens alex = register("Alex");
        Tokens bob = register("Bob");
        long workoutId = createWorkout(alex.access(), userId("Alex"), "Тренировка Alex");
        long id = addExerciseToWorkout(alex, workoutId, createExercise("Жим лёжа"), 1);

        mockMvc.perform(delete("/api/workout-exercise/" + id)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isForbidden());

        assertThat(countWorkoutExercises(workoutId)).isEqualTo(1);
    }

    @Test
    @DisplayName("An administrator can manage exercises in any workout.")
    void adminCanManageAnyWorkoutExercise() throws Exception {
        Tokens alex = register("Alex");
        Tokens admin = registerAdmin("Admin");
        long workoutId = createWorkout(alex.access(), userId("Alex"), "Тренировка Alex");
        long exerciseId = createExercise("Жим лёжа");


        long id = addExerciseToWorkout(admin, workoutId, exerciseId, 1);
        assertThat(countWorkoutExercises(workoutId)).isEqualTo(1);


        mockMvc.perform(get("/api/workout-exercise/" + id)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/workout-exercise/workout/" + workoutId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));


        mockMvc.perform(put("/api/workout-exercise/" + id)
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content("{\"orderIndex\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderIndex").value(3));


        mockMvc.perform(delete("/api/workout-exercise/" + id)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(countWorkoutExercises(workoutId)).isZero();
    }
}
