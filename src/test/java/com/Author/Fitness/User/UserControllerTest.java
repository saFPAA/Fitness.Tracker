package com.Author.Fitness.User;

import com.Author.Fitness.Auth.dto.AuthResponse;
import com.Author.Fitness.Exception.GlobalExceptionHandler;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.ClientIpResolver;
import com.Author.Fitness.User.dto.ChangePasswordRequest;
import com.Author.Fitness.User.dto.CreateUserRequest;
import com.Author.Fitness.User.dto.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService service;

    @Mock
    private PasswordService passwordService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(service, passwordService, new ClientIpResolver());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
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
    @DisplayName("Get a profile by ID.")
    void getUserById_returnsOk() throws Exception {
        UserResponse response = new UserResponse(1L, "current", "current@test.com", null);
        when(service.getUserById(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("current"));
    }

    @Test
    @DisplayName("Getting a nonexistent profile returns 404.")
    void getUserById_whenNotFound_returns404() throws Exception {
        when(service.getUserById(eq(1L), any())).thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("Getting a profile without permission returns 403.")
    void getUserById_whenAccessDenied_returns403() throws Exception {
        when(service.getUserById(eq(1L), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to access this resource"));

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Creating a user with a valid request returns a successful response.")
    void createUser_validRequest_returnsOk() throws Exception {
        CreateUserRequest request = new CreateUserRequest("newuser", "new@test.com", "password123");
        UserResponse response = new UserResponse(1L, "newuser", "new@test.com", null);
        when(service.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/user")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @DisplayName("Creating a user without a username returns 400.")
    void createUser_missingUsername_returns400() throws Exception {
        String invalidJson = """
                {
                    "username": "",
                    "email": "new@test.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/user")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Creating a user with a short password returns 400.")
    void createUser_shortPassword_returns400() throws Exception {
        String invalidJson = """
                {
                    "username": "newuser",
                    "email": "new@test.com",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/user")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Changes the current user's password and returns new tokens")
    void changeUserPassword_returnsNewTokens() throws Exception {
        ChangePasswordRequest request =
                new ChangePasswordRequest("currentPassword", "newPassword123");
        AuthResponse response =
                new AuthResponse("new-access-token", "new-refresh-token");

        when(passwordService.changePassword(eq(1L), any(), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/user/me/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        org.mockito.Mockito.verify(passwordService)
                .changePassword(eq(1L), eq(request), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Rejects a password change request longer than 72 UTF-8 bytes")
    void changePassword_passwordOver72Utf8Bytes_returns400() throws Exception {
        String tooLongPassword = "я".repeat(37);
        String json = """
            {
              "currentPassword": "current-password",
              "newPassword": "%s"
            }
            """.formatted(tooLongPassword);

        mockMvc.perform(patch("/api/user/me/password")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordService);
    }

    @Test
    @DisplayName("Deleting a user returns a successful response.")
    void deleteUser_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deleting a user without permission returns 403.")
    void deleteUser_whenAccessDenied_returns403() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("Denied"))
                .when(service).deleteUser(eq(1L), any());

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isForbidden());
    }
}
