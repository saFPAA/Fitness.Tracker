package com.Author.Fitness.Auth;

import com.Author.Fitness.User.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);
    }

    private UserEntity user() {
        return UserEntity.builder().id(1L).username("user").build();
    }

    private RefreshTokenEntity storedToken(boolean revoked, LocalDateTime expiresAt) {
        return RefreshTokenEntity.builder()
                .id(10L)
                .tokenHash("hash")
                .user(user())
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
    }

    @Test
    @DisplayName("Token creation stores a hash in the database and returns the raw token")
    void create_savesHashAndReturnsRawToken() {
        UserEntity user = user();

        String rawToken = service.create(user);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(repository).save(captor.capture());

        RefreshTokenEntity saved = captor.getValue();
        assertThat(rawToken).isNotBlank();
        assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(rawToken);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("Successful refresh revokes the old token and issues a new one")
    void rotate_whenValid_revokesOldAndIssuesNew() {
        RefreshTokenEntity stored = storedToken(false, LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(stored));

        RefreshTokenService.RotationResult result = service.rotate("raw-token");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo("raw-token");
        assertThat(result.user()).isEqualTo(stored.getUser());
        verify(repository).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("Unknown token is rejected")
    void rotate_whenTokenUnknown_throws() {
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Expired token is rejected and no new token is issued")
    void rotate_whenExpired_throwsAndDoesNotIssueNew() {
        RefreshTokenEntity stored = storedToken(false, LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.rotate("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Reusing a revoked token revokes the user’s other tokens")
    void rotate_whenAlreadyRevoked_revokesAllTokensOfUserAndThrows() {
        RefreshTokenEntity stored = storedToken(true, LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.rotate("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository).revokeAllByUserId(1L);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Logout revokes an existing token")
    void revoke_whenTokenExists_marksRevoked() {
        RefreshTokenEntity stored = storedToken(false, LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        service.revoke("raw-token");

        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Logout with an unknown token does not change any data")
    void revoke_whenTokenUnknown_doesNothing() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.revoke("unknown");

        verify(repository, never()).save(any());
    }
}
