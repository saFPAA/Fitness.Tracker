package com.Author.Fitness.User;


import com.Author.Fitness.Auth.RefreshTokenService;
import com.Author.Fitness.Auth.dto.AuthResponse;
import com.Author.Fitness.Exception.InvalidCurrentPasswordException;
import com.Author.Fitness.Exception.PasswordValidationException;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.JwtService;
import com.Author.Fitness.Security.LoginRateLimiter;
import com.Author.Fitness.User.dto.ChangePasswordRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    private PasswordService passwordService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(
                loginRateLimiter,
                userRepository,
                passwordEncoder,
                refreshTokenService,
                jwtService
        );
        when(loginRateLimiter.passwordChangeBlockedFor(anyLong(), any())).thenReturn(Duration.ZERO);

        user = UserEntity.builder()
                .id(1L)
                .username("alex")
                .email("alex@example.com")
                .password("old-hash")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Changes the password and returns new tokens")
    void changePassword_validRequest_updatesPasswordAndReturnsTokens() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("current-password", "new-password-123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("new-access");
        when(refreshTokenService.create(user)).thenReturn("new-refresh");

        AuthResponse result = passwordService.changePassword(1L, request, "127.0.0.1");

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");

        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(1L);
        verify(refreshTokenService).create(user);
    }

    @Test
    @DisplayName("Rejects a change when the current password is incorrect")
    void changePassword_incorrectCurrentPassword_throwsException() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("wrong-password", "new-password-123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> passwordService.changePassword(1L, request, "127.0.0.1"))
                .isInstanceOf(InvalidCurrentPasswordException.class);
    }

    @Test
    @DisplayName("Does not change the password or tokens when the current password is incorrect")
    void changePassword_incorrectCurrentPassword_doesNotChangePasswordOrTokens() {
        String oldHash = user.getPassword();
        ChangePasswordRequest request =
                new ChangePasswordRequest("wrong-password", "new-password-123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", oldHash)).thenReturn(false);

        assertThatThrownBy(() -> passwordService.changePassword(1L, request, "127.0.0.1"))
                .isInstanceOf(InvalidCurrentPasswordException.class);

        assertThat(user.getPassword()).isEqualTo(oldHash);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verifyNoInteractions(refreshTokenService, jwtService);
    }

    @Test
    @DisplayName("Fails when the user does not exist")
    void changePassword_userDoesNotExist_throwsException() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("current-password", "new-password-123");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordService.changePassword(1L, request, "127.0.0.1"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Does not revoke tokens when the user does not exist")
    void changePassword_userDoesNotExist_doesNotRevokeTokens() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("current-password", "new-password-123");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordService.changePassword(1L, request, "127.0.0.1"))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(passwordEncoder, refreshTokenService, jwtService);
        verify(userRepository, never()).save(any());
    }
}
