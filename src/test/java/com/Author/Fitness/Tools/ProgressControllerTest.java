package com.Author.Fitness.Tools;

import com.Author.Fitness.Exception.GlobalExceptionHandler;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.Role;
import com.Author.Fitness.User.UserEntity;
import com.Author.Fitness.WorkoutSet.dto.WorkoutSetResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    @Mock
    private ProgressService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProgressController controller = new ProgressController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UserEntity currentUserEntity = UserEntity.builder()
                .id(1L)
                .username("current")
                .password("hashed")
                .role(Role.USER)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(currentUserEntity);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Getting exercise progress returns a successful response.")
    void getProgressForExercise_returnsOk() throws Exception {
        WorkoutSetResponse response = new WorkoutSetResponse(1000L, 100L, 1, 10, BigDecimal.valueOf(50));
        when(service.getProgressForExercise(eq(1L), eq(5L), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/progress").param("userId", "1").param("exerciseId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repsDone").value(10));
    }

    @Test
    @DisplayName("Getting exercise progress without permission returns 403.")
    void getProgressForExercise_whenAccessDenied_returns403() throws Exception {
        when(service.getProgressForExercise(eq(2L), eq(5L), any()))
                .thenThrow(new AccessDeniedException("Denied"));

        mockMvc.perform(get("/api/progress").param("userId", "2").param("exerciseId", "5"))
                .andExpect(status().isForbidden());
    }
}