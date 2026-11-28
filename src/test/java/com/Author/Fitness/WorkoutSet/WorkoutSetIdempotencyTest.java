package com.Author.Fitness.WorkoutSet;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkoutSetIdempotencyTest extends AbstractIntegrationTest {

    @Test
    void concurrentRequestsWithTheSameKeyCreateOneSet() throws Exception {
        Tokens owner = register("owner");
        long exercise = workoutExercise(owner);
        UUID requestId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return create(owner, exercise, requestId, 10).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
            });
            var retry = executor.submit(() -> {
                start.await();
                return create(owner, exercise, requestId, 10).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
            });
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(retry.get(10, TimeUnit.SECONDS));
        }

        assertThat(setCount(exercise)).isEqualTo(1);
        create(owner, exercise, requestId, 8).andExpect(status().isConflict());
        create(owner, exercise, UUID.randomUUID(), 10).andExpect(status().isOk());
        assertThat(setCount(exercise)).isEqualTo(2);
    }

    @Test
    void retryAfterDeletionReturnsOriginalResponseWithoutRecreatingTheSet() throws Exception {
        Tokens owner = register("owner");
        long exercise = workoutExercise(owner);
        UUID requestId = UUID.randomUUID();
        String original = create(owner, exercise, requestId, 10).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long setId = jdbc.queryForObject("SELECT id FROM workout_sets WHERE workout_exercise_id = ?", Long.class, exercise);

        mockMvc.perform(delete("/api/workout-set/{id}", setId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        create(owner, exercise, requestId, 10).andExpect(status().isOk()).andExpect(content().json(original));
        assertThat(setCount(exercise)).isZero();
    }

    private long workoutExercise(Tokens owner) throws Exception {
        long workout = createWorkout(owner.access(), userId("owner"), "Training");
        return addExerciseToWorkout(owner, workout, createExercise("Test exercise"), 1);
    }

    private ResultActions create(Tokens owner, long exercise, UUID requestId, int reps) throws Exception {
        return mockMvc.perform(post("/api/workout-set").header("Authorization", bearer(owner))
                .contentType(APPLICATION_JSON).content("""
                        {"workoutExerciseId":%d,"position":1,"repsDone":%d,"weight":60.25,"requestId":"%s"}
                        """.formatted(exercise, reps, requestId)));
    }

    private Integer setCount(long exercise) {
        return jdbc.queryForObject("SELECT count(*) FROM workout_sets WHERE workout_exercise_id = ?", Integer.class, exercise);
    }
}
