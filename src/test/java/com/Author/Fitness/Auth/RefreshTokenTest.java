package com.Author.Fitness.Auth;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RefreshTokenTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("The database stores the refresh token’s hash, not the token itself")
    void refreshTokenIsStoredAsHash() throws Exception {
        Tokens token = register("Alex");

        String refreshToken = token.refresh();

        String storedTokenHash = jdbc.queryForObject(
                "SELECT token_hash FROM refresh_tokens WHERE user_id = ?",
                String.class,
                userId("Alex")
        );

        assertNotNull(storedTokenHash);
        assertNotEquals(refreshToken, storedTokenHash);
        assertEquals(64, storedTokenHash.length());
    }

    @Test
    @DisplayName("Refresh revokes the old token and issues a new one")
    void refreshRotatesToken() throws Exception {
        Tokens initialTokens = register("Alex");

        Tokens rotatedTokens = refresh(initialTokens.refresh());

        assertNotEquals(initialTokens.refresh(), rotatedTokens.refresh());
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked = true",
                Integer.class,
                userId("Alex")
        ));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked = false",
                Integer.class,
                userId("Alex")
        ));

        Tokens nextTokens = refresh(rotatedTokens.refresh());
        assertNotEquals(rotatedTokens.refresh(), nextTokens.refresh());
    }

    @Test
    @DisplayName("Expired refresh token is rejected with 401")
    void expiredRefreshTokenIsRejected() throws Exception {
        Tokens tokens = register("Alex");
        jdbc.update("UPDATE refresh_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE user_id = ?",
                userId("Alex"));

        refreshRaw(tokens.refresh()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unknown refresh token is rejected with 401")
    void unknownRefreshTokenIsRejected() throws Exception {
        refreshRaw("unknown-refresh-token").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Blank refresh token is rejected with 400")
    void blankRefreshTokenIsRejected() throws Exception {
        refreshRaw("").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Logout revokes the refresh token")
    void logoutRevokesRefreshToken() throws Exception {
        Tokens tokens = register("Alex");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refresh())))
                .andExpect(status().isOk());

        assertEquals(0, activeRefreshTokens());
        refreshRaw(tokens.refresh()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout with an unknown token does not fail")
    void logoutWithUnknownTokenDoesNotFail() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"unknown-refresh-token"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Revoked refresh token cannot be used")
    void revokedTokenCannotBeUsed() throws Exception {
        Tokens tokens = register("RevokedUser");
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refresh())))
                .andExpect(status().isOk());

        refreshRaw(tokens.refresh()).andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("Of two simultaneous refresh requests using the same token, only one succeeds")
    void concurrentRefreshWithSameTokenAllowsOnlyOne() throws Exception {
        Tokens tokens = register("ConcurrentUser");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> concurrentRefreshStatus(tokens.refresh(), ready, start));
            Future<Integer> second = executor.submit(() -> concurrentRefreshStatus(tokens.refresh(), ready, start));

            ready.await();
            start.countDown();

            int firstStatus = first.get();
            int secondStatus = second.get();
            assertTrue((firstStatus == 200 && secondStatus == 401)
                            || (firstStatus == 401 && secondStatus == 200),
                    "Ожидался один успешный ответ и один 401, получены: " + firstStatus + " и " + secondStatus);
        }
    }

    @Test
    @DisplayName("Rejects the refresh token issued before the password change")
    void oldRefreshTokenIsRejectedAfterPasswordChange() throws Exception {
        Tokens oldTokens = register("PasswordUser");

        mockMvc.perform(patch("/api/user/me/password")
                        .header("Authorization", bearer(oldTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "currentPassword": "password123",
                              "newPassword": "new-password-123"
                            }
                            """))
                .andExpect(status().isOk());

        refreshRaw(oldTokens.refresh())
                .andExpect(status().isUnauthorized());
    }

    private int concurrentRefreshStatus(String refreshToken, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        MvcResult result = refreshRaw(refreshToken).andReturn();
        return result.getResponse().getStatus();
    }

}
