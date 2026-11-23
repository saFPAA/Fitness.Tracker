package com.Author.Fitness.Auth;

import com.Author.Fitness.Security.LoginRateLimitProperties;
import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginRateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LoginRateLimitProperties rateLimitProperties;

    private ResultActions loginAttempt(String username, String password, String ip) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        MockHttpServletRequestBuilder request = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(mockRequest -> {
                    mockRequest.setRemoteAddr(ip);
                    return mockRequest;
                });

        return mockMvc.perform(request);
    }

    private ResultActions registerAttempt(String username, String ip) throws Exception {
        String body = """
                {"username":"%s","email":"%s@example.com","password":"password123"}
                """.formatted(username, username);

        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(mockRequest -> {
                    mockRequest.setRemoteAddr(ip);
                    return mockRequest;
                }));
    }

    private ResultActions passwordChangeAttempt(String accessToken, String currentPassword, String ip)
            throws Exception {
        String body = """
                {"currentPassword":"%s","newPassword":"newPassword123"}
                """.formatted(currentPassword);

        return mockMvc.perform(patch("/api/user/me/password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(mockRequest -> {
                    mockRequest.setRemoteAddr(ip);
                    return mockRequest;
                }));
    }

    @Test
    @DisplayName("Registration requests from one IP are limited")
    void registrationLimit_blocksRequestsFromSameIp() throws Exception {
        int originalLimit = rateLimitProperties.getMaxRegistrationAttemptsPerIp();
        rateLimitProperties.setMaxRegistrationAttemptsPerIp(2);

        try {
            registerAttempt("RegisterA", "192.0.2.20").andExpect(status().isOk());
            registerAttempt("RegisterB", "192.0.2.20").andExpect(status().isOk());
            registerAttempt("RegisterC", "192.0.2.20").andExpect(status().isTooManyRequests());
            registerAttempt("RegisterD", "192.0.2.21").andExpect(status().isOk());
        } finally {
            rateLimitProperties.setMaxRegistrationAttemptsPerIp(originalLimit);
        }
    }

    @Test
    @DisplayName("Incorrect current passwords are limited per user and IP")
    void passwordChangeLimit_blocksAfterRepeatedIncorrectCurrentPasswords() throws Exception {
        var tokens = register("PasswordLimitUser");
        int originalLimit = rateLimitProperties.getMaxFailedPasswordChangesPerUserIp();
        rateLimitProperties.setMaxFailedPasswordChangesPerUserIp(2);

        try {
            passwordChangeAttempt(tokens.access(), "wrong-password", "192.0.2.22")
                    .andExpect(status().isBadRequest());
            passwordChangeAttempt(tokens.access(), "wrong-password", "192.0.2.22")
                    .andExpect(status().isBadRequest());
            passwordChangeAttempt(tokens.access(), "password123", "192.0.2.22")
                    .andExpect(status().isTooManyRequests());
        } finally {
            rateLimitProperties.setMaxFailedPasswordChangesPerUserIp(originalLimit);
        }
    }

    @Test
    @DisplayName("Blocks login after the configured number of failed attempts and returns Retry-After")
    void repeatedFailures_blockEvenCorrectPasswordAndIncludeRetryAfter() throws Exception {
        register("LimitUser");

        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttempt("LimitUser", "wrong-password", "192.0.2.10")
                    .andExpect(status().isUnauthorized());
        }

        MvcResult result = loginAttempt("LimitUser", "password123", "192.0.2.10")
                .andExpect(status().isTooManyRequests())
                .andReturn();

        String retryAfter = result.getResponse().getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isPositive();
    }

    @Test
    @DisplayName("A successful login resets the failed-attempt counter for the login and IP")
    void successfulLogin_resetsPairCounter() throws Exception {
        register("ResetUser");

        for (int attempt = 0; attempt < 4; attempt++) {
            loginAttempt("ResetUser", "wrong-password", "192.0.2.11")
                    .andExpect(status().isUnauthorized());
        }

        loginAttempt("ResetUser", "password123", "192.0.2.11")
                .andExpect(status().isOk());

        for (int attempt = 0; attempt < 4; attempt++) {
            loginAttempt("ResetUser", "wrong-password", "192.0.2.11")
                    .andExpect(status().isUnauthorized());
        }

        loginAttempt("ResetUser", "password123", "192.0.2.11")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Failed attempts for one login and IP do not block another login or IP")
    void pairLimit_doesNotAffectOtherLoginOrIp() throws Exception {
        register("AccountA");
        register("AccountB");

        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttempt("AccountA", "wrong-password", "192.0.2.12")
                    .andExpect(status().isUnauthorized());
        }

        loginAttempt("AccountA", "password123", "192.0.2.13")
                .andExpect(status().isOk());
        loginAttempt("AccountB", "password123", "192.0.2.12")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Failed attempts for an unknown username are counted")
    void unknownUsernameFailures_areCounted() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttempt("FutureUser", "wrong-password", "192.0.2.14")
                    .andExpect(status().isUnauthorized());
        }

        register("FutureUser");

        loginAttempt("FutureUser", "password123", "192.0.2.14")
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("The IP-wide limit applies across different usernames")
    void ipLimit_appliesAcrossUsernames() throws Exception {
        register("IpLimitUser");
        int originalLimit = rateLimitProperties.getMaxAttemptsPerIp();
        rateLimitProperties.setMaxAttemptsPerIp(2);

        try {
            loginAttempt("UnknownA", "wrong-password", "192.0.2.15")
                    .andExpect(status().isUnauthorized());
            loginAttempt("UnknownB", "wrong-password", "192.0.2.15")
                    .andExpect(status().isUnauthorized());

            loginAttempt("IpLimitUser", "password123", "192.0.2.15")
                    .andExpect(status().isTooManyRequests());
        } finally {
            rateLimitProperties.setMaxAttemptsPerIp(originalLimit);
        }
    }

    @Test
    @DisplayName("The login block expires after the Redis window")
    void blockExpiresAfterWindow() throws Exception {
        register("ExpiryUser");
        Duration originalWindow = rateLimitProperties.getWindow();
        rateLimitProperties.setWindow(Duration.ofSeconds(3));

        try {
            for (int attempt = 0; attempt < 5; attempt++) {
                loginAttempt("ExpiryUser", "wrong-password", "192.0.2.16")
                        .andExpect(status().isUnauthorized());
            }

            loginAttempt("ExpiryUser", "password123", "192.0.2.16")
                    .andExpect(status().isTooManyRequests());

            Thread.sleep(3_200);

            loginAttempt("ExpiryUser", "password123", "192.0.2.16")
                    .andExpect(status().isOk());
        } finally {
            rateLimitProperties.setWindow(originalWindow);
        }
    }

    @Test
    @DisplayName("Parallel failed logins: exactly the configured number reach authentication")
    void parallelFailures_onlyLimitedNumberAreChecked() throws Exception {
        register("RaceUser");
        int limit = rateLimitProperties.getMaxAttemptsPerLoginIp();
        int threads = 20;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return loginAttempt("RaceUser", "wrong-password", "192.0.2.40")
                            .andReturn().getResponse().getStatus();
                }));
            }

            ready.await();
            start.countDown();

            int unauthorized = 0;
            int tooMany = 0;
            for (Future<Integer> future : futures) {
                int httpStatus = future.get(30, TimeUnit.SECONDS);
                if (httpStatus == 401) {
                    unauthorized++;
                } else if (httpStatus == 429) {
                    tooMany++;
                }
            }

            assertThat(unauthorized).isEqualTo(limit);
            assertThat(tooMany).isEqualTo(threads - limit);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Successful logins do not consume the IP limit")
    void successfulLogins_doNotConsumeIpLimit() throws Exception {
        register("NatUserA");
        register("NatUserB");
        int originalLimit = rateLimitProperties.getMaxAttemptsPerIp();
        rateLimitProperties.setMaxAttemptsPerIp(3);

        try {
            for (int attempt = 0; attempt < 6; attempt++) {
                String username = attempt % 2 == 0 ? "NatUserA" : "NatUserB";
                loginAttempt(username, "password123", "192.0.2.30")
                        .andExpect(status().isOk());
            }
        } finally {
            rateLimitProperties.setMaxAttemptsPerIp(originalLimit);
        }
    }

    @Test
    @DisplayName("A successful login releases only its own IP reservation, earlier IP failures stay")
    void successfulLogin_keepsEarlierIpFailures() throws Exception {
        register("IpKeepUser");
        int originalLimit = rateLimitProperties.getMaxAttemptsPerIp();
        rateLimitProperties.setMaxAttemptsPerIp(3);

        try {
            loginAttempt("UnknownA", "wrong-password", "192.0.2.31")
                    .andExpect(status().isUnauthorized());          // IP: 1
            loginAttempt("UnknownB", "wrong-password", "192.0.2.31")
                    .andExpect(status().isUnauthorized());          // IP: 2
            loginAttempt("IpKeepUser", "password123", "192.0.2.31")
                    .andExpect(status().isOk());                    // резерв 3, снятие -> 2
            loginAttempt("UnknownC", "wrong-password", "192.0.2.31")
                    .andExpect(status().isUnauthorized());          // IP: 3
            loginAttempt("UnknownD", "wrong-password", "192.0.2.31")
                    .andExpect(status().isTooManyRequests());       // лимит достигнут
        } finally {
            rateLimitProperties.setMaxAttemptsPerIp(originalLimit);
        }
    }

    @Test
    @DisplayName("The per-login counter across many IPs only logs and never blocks")
    void perLoginCounter_doesNotBlockLogin() throws Exception {
        register("SoftUser");
        int originalLimit = rateLimitProperties.getMaxAttemptsPerLogin();
        rateLimitProperties.setMaxAttemptsPerLogin(2);

        try {
            loginAttempt("SoftUser", "wrong-password", "192.0.2.32")
                    .andExpect(status().isUnauthorized());
            loginAttempt("SoftUser", "wrong-password", "192.0.2.33")
                    .andExpect(status().isUnauthorized());
            loginAttempt("SoftUser", "wrong-password", "192.0.2.34")
                    .andExpect(status().isUnauthorized());
            loginAttempt("SoftUser", "wrong-password", "192.0.2.35")
                    .andExpect(status().isUnauthorized());

            loginAttempt("SoftUser", "password123", "192.0.2.36")
                    .andExpect(status().isOk());
        } finally {
            rateLimitProperties.setMaxAttemptsPerLogin(originalLimit);
        }
    }

    @Test
    @DisplayName("IPv6 addresses from one /64 subnet share the same limit")
    void ipv6_sameSubnetSharesLimit_differentSubnetDoesNot() throws Exception {
        register("V6User");

        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttempt("V6User", "wrong-password", "2001:db8:abcd:12::1")
                    .andExpect(status().isUnauthorized());
        }

        loginAttempt("V6User", "password123", "2001:db8:abcd:12::2")
                .andExpect(status().isTooManyRequests());

        loginAttempt("V6User", "password123", "2001:db8:abcd:13::1")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A successful password change clears the failed password-change counter")
    void passwordChangeSuccess_clearsCounter() throws Exception {
        register("PwdClearUser");
        int originalLimit = rateLimitProperties.getMaxFailedPasswordChangesPerUserIp();
        rateLimitProperties.setMaxFailedPasswordChangesPerUserIp(2);

        try {
            String token = login("PwdClearUser", "password123").access();

            passwordChangeAttempt(token, "wrong-password", "192.0.2.37")
                    .andExpect(status().isBadRequest());
            passwordChangeAttempt(token, "password123", "192.0.2.37")
                    .andExpect(status().isOk());

            String freshToken = login("PwdClearUser", "newPassword123").access();

            passwordChangeAttempt(freshToken, "wrong-password", "192.0.2.37")
                    .andExpect(status().isBadRequest());
            passwordChangeAttempt(freshToken, "wrong-password", "192.0.2.37")
                    .andExpect(status().isBadRequest());
            passwordChangeAttempt(freshToken, "wrong-password", "192.0.2.37")
                    .andExpect(status().isTooManyRequests());
        } finally {
            rateLimitProperties.setMaxFailedPasswordChangesPerUserIp(originalLimit);
        }
    }

}