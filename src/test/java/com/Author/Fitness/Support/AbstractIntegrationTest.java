package com.Author.Fitness.Support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-test-secret-test-secret-123456",
        "DB_USERNAME=test", "DB_PASSWORD=test"
})
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
public abstract class AbstractIntegrationTest {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    }

    protected record Tokens(String access, String refresh) {}

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected StringRedisTemplate redis;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        redis.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    protected Tokens register(String username) throws Exception {
        String body = """
                {"username":"%s","email":"%s@example.com","password":"password123"}
                """.formatted(username, username);
        return tokensFrom(mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk()));
    }

    protected Tokens login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        return tokensFrom(mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()));
    }

    //Администратор: регистрируем обычного пользователя, повышаем роль в базе и входим заново за свежим токеном
    protected Tokens registerAdmin(String username) throws Exception {
        register(username);
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", username);
        return login(username, "password123");
    }

    protected String adminCreateAndGetAccessToken(String username) throws Exception {
        return registerAdmin(username).access();
    }

    protected String userCreateAndGetAccessToken(String username) throws Exception {
        register(username);
        return login(username, "password123").access();
    }

    protected long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    protected static String bearer(Tokens tokens) {
        return "Bearer " + tokens.access();
    }

    //Обновление токена, которое должно пройти успешно.
    protected Tokens refresh(String refreshToken) throws Exception {
        return tokensFrom(refreshRaw(refreshToken).andExpect(status().isOk()));
    }

    //Обновление токена без проверки ответа
    protected ResultActions refreshRaw(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    protected int activeRefreshTokens() {
        return jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE revoked = false", Integer.class);
    }


    protected long createWorkout(String accessToken, long userId, String name) throws Exception {
        String body = """
                {"name":"%s","date":"2026-10-27","userId":%d}
                """.formatted(name, userId);

        String json = mockMvc.perform(post("/api/workout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number id = JsonPath.read(json, "$.id");
        return id.longValue();
    }

    //Упражнение каталога
    protected long createExercise(String name) {
        return jdbc.queryForObject(
                "INSERT INTO exercises (name, muscle_group) VALUES (?, 'Грудь') RETURNING id",
                Long.class, name);
    }


    protected long addExerciseToWorkout(Tokens owner, long workoutId, long exerciseId, int orderIndex) throws Exception {
        String json = mockMvc.perform(post("/api/workout-exercise")
                        .header("Authorization", bearer(owner))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"workoutId":%d,"exerciseId":%d,"orderIndex":%d}
                                """.formatted(workoutId, exerciseId, orderIndex)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number id = JsonPath.read(json, "$.id");
        return id.longValue();
    }

    protected int countWorkoutExercises(long workoutId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM workout_exercises WHERE workout_id = ?", Integer.class, workoutId);
    }

    private Tokens tokensFrom(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        String access = JsonPath.read(json, "$.accessToken");
        String refresh = JsonPath.read(json, "$.refreshToken");
        return new Tokens(access, refresh);
    }
}
