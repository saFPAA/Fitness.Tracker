package com.Author.Fitness.Workout;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class WorkoutAccessTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("The owner can read their own workout.")
    void ownerCanReadOwnWorkout() throws Exception {
        Tokens alex = register("Alex");
        long alexId = userId("Alex");
        long workoutId = createWorkout(alex.access(), alexId, "Тренировка на грудь");

        mockMvc.perform(get("/api/workout/" + workoutId)
                        .header("Authorization", "Bearer " + alex.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workoutId))
                .andExpect(jsonPath("$.name").value("Тренировка на грудь"))
                .andExpect(jsonPath("$.userId").value(alexId));
    }

    @Test
    @DisplayName("A user cannot read another user’s workout and receives 403.")
    void userCannotReadOtherUsersWorkout() throws Exception{

        String user1AccessToken = userCreateAndGetAccessToken("Alex");
        String user2AccessToken = userCreateAndGetAccessToken("Bob");
        long workoutId = createWorkout(user1AccessToken,userId("Alex"),"Тренировка на грудь");

        mockMvc.perform(get("/api/workout/" + workoutId)
                .header("Authorization","Bearer "+user2AccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot list another user’s workouts and receives 403.")
    void userCannotListOtherUsersWorkouts() throws Exception{
        String user1AccessToken = userCreateAndGetAccessToken("Alex");
        String user2AccessToken = userCreateAndGetAccessToken("Bob");
        createWorkout(user1AccessToken,userId("Alex"),"Тренировка на грудь1");
        createWorkout(user1AccessToken,userId("Alex"),"Тренировка на грудь2");

        mockMvc.perform(get("/api/workout/user/" + userId("Alex"))
                        .header("Authorization","Bearer "+user2AccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot create a workout on behalf of another user and receives 403.")
    void userCannotCreateWorkoutForAnotherUser() throws Exception {
        String alexToken = userCreateAndGetAccessToken("Alex");
        userCreateAndGetAccessToken("Bob");

        mockMvc.perform(post("/api/workout")
                        .header("Authorization", "Bearer " + alexToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Тренировка Bob",
                                "date": "2026-10-27",
                                "userId": %d
                            }
                            """.formatted(userId("Bob"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot update another user’s workout and receives 403.")
    void userCannotUpdateOtherUsersWorkout() throws Exception {
        String alexToken = userCreateAndGetAccessToken("Alex");
        String bobToken = userCreateAndGetAccessToken("Bob");

        long workoutId = createWorkout(alexToken, userId("Alex"), "Тренировка Alex");

        mockMvc.perform(put("/api/workout/" + workoutId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Изменённая тренировка",
                                "date": "2026-10-27",
                                "userId": %d
                            }
                            """.formatted(userId("Alex"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot delete another user’s workout and receives 403.")
    void userCannotDeleteOtherUsersWorkout() throws Exception {
        String alexToken = userCreateAndGetAccessToken("Alex");
        String bobToken = userCreateAndGetAccessToken("Bob");

        long workoutId = createWorkout(alexToken, userId("Alex"), "Тренировка Alex");

        mockMvc.perform(delete("/api/workout/" + workoutId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An administrator can read any workout.")
    void adminCanReadAnyWorkout() throws Exception {
        String alexToken = userCreateAndGetAccessToken("Alex");
        String adminToken = adminCreateAndGetAccessToken("Admin");

        long alexId = userId("Alex");
        long workoutId = createWorkout(alexToken, alexId, "Тренировка Alex");

        mockMvc.perform(get("/api/workout/" + workoutId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workoutId))
                .andExpect(jsonPath("$.name").value("Тренировка Alex"))
                .andExpect(jsonPath("$.userId").value(alexId));
    }

    @Test
    @DisplayName("An administrator can delete any workout.")
    void adminCanDeleteAnyWorkout() throws Exception {
        String alexToken = userCreateAndGetAccessToken("Alex");
        String adminToken = adminCreateAndGetAccessToken("Admin");

        long workoutId = createWorkout(alexToken, userId("Alex"), "Тренировка Alex");

        mockMvc.perform(delete("/api/workout/" + workoutId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workouts WHERE id = ?",
                Integer.class,
                workoutId
        );

        Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("Workouts are listed from newest to oldest.")
    void workoutsAreListedNewestFirst() throws Exception {
        String accessToken = userCreateAndGetAccessToken("Alex");
        long alexId = userId("Alex");

        mockMvc.perform(post("/api/workout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Старая тренировка",
                                "date": "2026-10-25",
                                "userId": %d
                            }
                            """.formatted(alexId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Новая тренировка",
                                "date": "2026-10-27",
                                "userId": %d
                            }
                            """.formatted(alexId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/workout/user/" + alexId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Новая тренировка"))
                .andExpect(jsonPath("$.content[1].name").value("Старая тренировка"));
    }

    @Test
    @DisplayName("A page size above 100 returns 400 instead of 500.")
    void pageSizeAboveLimitIsRejected() throws Exception {
        String accessToken = userCreateAndGetAccessToken("Alex");

        mockMvc.perform(get("/api/workout/user/" + userId("Alex"))
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A negative page number returns 400 instead of 500.")
    void negativePageNumberIsRejected() throws Exception {
        String accessToken = userCreateAndGetAccessToken("Alex");

        mockMvc.perform(get("/api/workout/user/" + userId("Alex"))
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNumber", "-1"))
                .andExpect(status().isBadRequest());
    }

}
