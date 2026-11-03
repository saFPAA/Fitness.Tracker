package com.Author.Fitness.Security;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityAccessTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Protected endpoints reject requests without a token with 401")
    void anonymousIsRejectedOnProtectedEndpoints() throws Exception{

        mockMvc.perform(get("/api/user/6")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("Authentication endpoints are accessible without a token")
    void anonymousCanAccessAuthEndpoints() throws Exception{
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alex","email":"Alex@example.com","password":"password123"}
                         """))
                .andExpect(status().isOk());

        //проверка на то, что пользователь создан
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                "Alex@example.com");

        Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Arbitrary string used as a token is rejected with 401")
    void invalidTokenIsRejected() throws Exception{

        mockMvc.perform(get("/api/exercise")
                        .header("Authorization","Bearer " + "invalid-token"))
                .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("A tampered token with an invalid signature is rejected with 401")
    void tamperedTokenIsRejected() throws Exception{
        String accessToken = userCreateAndGetAccessToken("Alex");

        String tamperedToken = accessToken + "tampered";

        mockMvc.perform(get("/api/exercise")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A regular user cannot create an exercise and receives 403")
    void userCannotCreateExercise() throws Exception{

        String accessToken = userCreateAndGetAccessToken("Alex");

        mockMvc.perform(post("/api/exercise")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        { "name":"Жим лежа","muscleGroup":"Грудь","equipment":"Штанга"}
                        """))
                .andExpect(status().isForbidden());

        //проверка на то, что упражнение не создано
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM exercises WHERE name = ?",
                Integer.class,
                "Жим лежа");

        Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("A regular user cannot update an exercise and receives 403")
    void userCannotUpdateExercise() throws Exception{
        String accessToken = userCreateAndGetAccessToken("Alex");

        Long exerciseId = jdbc.queryForObject("""
        INSERT INTO exercises (name, muscle_group, equipment)
        VALUES (?, ?, ?)
        RETURNING id
        """,
                Long.class,
                "Приседания",
                "Ноги",
                "Штанга"
        );

        mockMvc.perform(put("/api/exercise/%s".formatted(exerciseId))
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        { "name":"Жим лежа","muscleGroup":"Грудь","equipment":"Штанга"}
                        """))
                .andExpect(status().isForbidden());

        //проверяем неизменность
        String name = jdbc.queryForObject(
                "SELECT name FROM exercises WHERE id = ?",
                String.class,
                exerciseId
        );

        Assertions.assertThat(name).isEqualTo("Приседания");
    }

    @Test
    @DisplayName("A regular user cannot delete an exercise and receives 403")
    void userCannotDeleteExercise() throws Exception{
        String accessToken = userCreateAndGetAccessToken("Alex");

        //id для проверки
        Long exerciseId = jdbc.queryForObject("""
            INSERT INTO exercises (name, muscle_group, equipment)
            VALUES (?, ?, ?)
            RETURNING id
            """,
                Long.class,
                "Приседания",
                "Ноги",
                "Штанга"
        );

        mockMvc.perform(delete("/api/exercise/%s".formatted(exerciseId))
                        .header("Authorization","Bearer "+accessToken))
                .andExpect(status().isForbidden());

        // Проверяем что упражнение осталось в базе
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercises WHERE id = ?",
                Integer.class,
                exerciseId
        );

        Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("An administrator can create an exercise")
    void adminCanCreateExercise() throws Exception {
        String accessToken = adminCreateAndGetAccessToken("Alex");

        // создаю упражнение
        mockMvc.perform(post("/api/exercise")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Жим лежа6",
                                "muscleGroup": "Грудь",
                                "equipment": "Штанга"
                            }
                            """))
                .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercises WHERE name = ?",
                Integer.class,
                "Жим лежа6"
        );

        Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("An administrator can update an exercise")
    void adminCanUpdateExercise() throws Exception{
        String accessToken = adminCreateAndGetAccessToken("Alex");

        Long exerciseId = jdbc.queryForObject("""
            INSERT INTO exercises (name, muscle_group, equipment)
            VALUES (?, ?, ?)
            RETURNING id
            """,
                Long.class,
                "Приседания",
                "Ноги",
                "Штанга"
        );

        mockMvc.perform(put("/api/exercise/%s".formatted(exerciseId))
                        .header("Authorization","Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Жим лежа7",
                                "muscleGroup": "Грудь",
                                "equipment": "Штанга"
                            }
                            """))
                .andExpect(status().isOk());

        String name = jdbc.queryForObject(
                "SELECT name FROM exercises WHERE id = ?",
                String.class,
                exerciseId
        );

        Assertions.assertThat(name).isEqualTo("Жим лежа7");

    }

    @Test
    @DisplayName("An administrator can delete an exercise")
    void adminCanDeleteExercise() throws Exception{

        String accessToken = adminCreateAndGetAccessToken("Alex");

        Long exerciseId = jdbc.queryForObject("""
            INSERT INTO exercises (name, muscle_group, equipment)
            VALUES (?, ?, ?)
            RETURNING id
            """,
                Long.class,
                "Приседания10",
                "Ноги",
                "Штанга"
        );

        mockMvc.perform(delete("/api/exercise/%s".formatted(exerciseId))
                        .header("Authorization","Bearer "+accessToken))
                .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercises WHERE id = ?",
                Integer.class,
                exerciseId
        );

        Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("A regular user cannot create a user through POST /api/user and receives 403")
    void userCannotCreateUserViaAdminEndpoint() throws Exception{

        String accessToken = userCreateAndGetAccessToken("Alex");

        mockMvc.perform(post("/api/user")
                        .header("Authorization","Bearer "+accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alex2","email":"Alex@example.com","password":"password123"}
                         """))
                .andExpect(status().isForbidden());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "Alex2"
        );

        Assertions.assertThat(count).isZero();
    }


    @Test
    @DisplayName("An administrator can create a user through POST /api/user")
    void adminCanCreateUserViaAdminEndpoint() throws Exception {
        String accessToken = adminCreateAndGetAccessToken("Alex");

        mockMvc.perform(post("/api/user")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "Bob",
                                "email": "Bob@example.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "Bob"
        );

        Assertions.assertThat(count).isEqualTo(1);
    }





}
