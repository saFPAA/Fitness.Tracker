package com.Author.Fitness.Auth;


import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Incorrect password:401 with clear error message")
    void wrongPasswordIsRejected() throws Exception {
        register("alex");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alex","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Refresh token returns a new token pair, and the new access token works")
    void refreshIssuesWorkingTokens() throws Exception {
        Tokens first = register("alex");

        Tokens second = refresh(first.refresh());

        assertThat(second.refresh()).isNotEqualTo(first.refresh());
        mockMvc.perform(get("/api/exercise").header("Authorization", "Bearer " + second.access()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exercise"))          // без токена
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reusing an old refresh token revokes all of the user’s tokens.")
    void reusedRefreshTokenRevokesEverything() throws Exception {
        Tokens first = register("alex");
        Tokens second = refresh(first.refresh());
        assertThat(activeRefreshTokens()).isEqualTo(1);

        refreshRaw(first.refresh()).andExpect(status().isUnauthorized());

        refreshRaw(second.refresh()).andExpect(status().isUnauthorized());
        assertThat(activeRefreshTokens()).isZero();
    }
}
