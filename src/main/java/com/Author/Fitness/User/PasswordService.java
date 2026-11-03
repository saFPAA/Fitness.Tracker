package com.Author.Fitness.User;

import com.Author.Fitness.Auth.RefreshTokenService;
import com.Author.Fitness.Auth.dto.AuthResponse;
import com.Author.Fitness.Exception.InvalidCurrentPasswordException;
import com.Author.Fitness.Exception.TooManyAttemptsException;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.JwtService;
import com.Author.Fitness.Security.LoginRateLimiter;
import com.Author.Fitness.User.dto.ChangePasswordRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;


@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final LoginRateLimiter loginRateLimiter;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse changePassword(
            Long currentUserId,
            ChangePasswordRequest request,
            String clientIp
    ){
        Duration retryAfter = passwordChangeBlockedFor(currentUserId, clientIp);
        if (!retryAfter.isZero()) {
            throw new TooManyAttemptsException(retryAfter);
        }

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
            recordPasswordChangeFailure(currentUserId, clientIp);
            throw new InvalidCurrentPasswordException();
        }


        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        recordPasswordChangeSuccess(currentUserId, clientIp);

        refreshTokenService.revokeAllForUser(currentUserId);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.create(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    private Duration passwordChangeBlockedFor(Long userId, String clientIp) {
        try {
            return loginRateLimiter.passwordChangeBlockedFor(userId, clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; password-change rate limiting is bypassed", e);
            return Duration.ZERO;
        }
    }

    private void recordPasswordChangeFailure(Long userId, String clientIp) {
        try {
            loginRateLimiter.recordPasswordChangeFailure(userId, clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; failed password-change attempt was not recorded", e);
        }
    }

    private void recordPasswordChangeSuccess(Long userId, String clientIp) {
        try {
            loginRateLimiter.recordPasswordChangeSuccess(userId, clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; password-change counter was not cleared", e);
        }
    }

}
