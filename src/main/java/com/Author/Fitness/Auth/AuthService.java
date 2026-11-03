package com.Author.Fitness.Auth;

import com.Author.Fitness.Auth.dto.AuthResponse;
import com.Author.Fitness.Auth.dto.LoginRequest;
import com.Author.Fitness.Auth.dto.RefreshRequest;
import com.Author.Fitness.Exception.TooManyAttemptsException;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.Security.CustomUserDetailsService;
import com.Author.Fitness.Security.JwtService;
import com.Author.Fitness.Security.LoginRateLimiter;
import com.Author.Fitness.User.UserService;
import com.Author.Fitness.User.dto.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final LoginRateLimiter loginRateLimiter;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(CreateUserRequest request, String clientIp){
        Duration retryAfter = registrationBlockedFor(clientIp);
        if (!retryAfter.isZero()) {
            throw new TooManyAttemptsException(retryAfter);
        }

        recordRegistrationAttempt(clientIp);
        userService.createUser(request);

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(request.username());

        log.info("User registered and authenticated: {}", request.username());
        return issueTokens(userDetails);
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        Duration retryAfter = blockedFor(request.username(), clientIp);

        if (!retryAfter.isZero()) {
            throw new TooManyAttemptsException(retryAfter);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            recordFailure(request.username(), clientIp);
            throw e;
        }

        recordSuccess(request.username(), clientIp);

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        log.info("User logged in: {}", request.username());
        return issueTokens(userDetails);
    }

    private Duration blockedFor(String username, String clientIp) {
        try {
            return loginRateLimiter.blockedFor(username, clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; login rate limiting is bypassed", e);
            return Duration.ZERO;
        }
    }

    private void recordFailure(String username, String clientIp) {
        try {
            loginRateLimiter.recordFailure(username, clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; failed login attempt was not recorded", e);
        }
    }

    private void recordSuccess(String username, String clientIp) {
        try {
            loginRateLimiter.recordSuccess(username, clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; successful login counter was not cleared", e);
        }
    }

    private Duration registrationBlockedFor(String clientIp) {
        try {
            return loginRateLimiter.registrationBlockedFor(clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; registration rate limiting is bypassed", e);
            return Duration.ZERO;
        }
    }

    private void recordRegistrationAttempt(String clientIp) {
        try {
            loginRateLimiter.recordRegistrationAttempt(clientIp);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable; registration attempt was not recorded", e);
        }
    }

    public AuthResponse refresh(RefreshRequest request){
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.refreshToken());

        String accessToken = jwtService.generateToken(new CustomUserDetails(result.user()));
        return new AuthResponse(accessToken, result.refreshToken());
    }

    public void logout(RefreshRequest request){
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse issueTokens(CustomUserDetails userDetails){
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.create(userDetails.getUser());
        return new AuthResponse(accessToken, refreshToken);
    }
}
