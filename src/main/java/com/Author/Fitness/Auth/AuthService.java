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
import org.springframework.dao.DataAccessException;
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
        Duration retryAfter = acquireRegistrationAttempt(clientIp);
        if (!retryAfter.isZero()) {
            throw new TooManyAttemptsException(retryAfter);
        }

        userService.createUser(request);

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(request.username());

        log.info("User registered and authenticated: {}", request.username());
        return issueTokens(userDetails);
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        Duration retryAfter = acquireLoginAttempt(request.username(), clientIp);

        if (!retryAfter.isZero()) {
            throw new TooManyAttemptsException(retryAfter);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        recordSuccess(request.username(), clientIp);

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        log.info("User logged in: {}", request.username());
        return issueTokens(userDetails);
    }

    private Duration acquireLoginAttempt(String username, String clientIp) {
        try {
            return loginRateLimiter.acquireLoginAttempt(username, clientIp);
        } catch (DataAccessException e) {
            log.error("Redis unavailable; login rate limiting is bypassed", e);
            return Duration.ZERO;
        }
    }

    private void recordSuccess(String username, String clientIp) {
        try {
            loginRateLimiter.recordSuccess(username, clientIp);
        } catch (DataAccessException e) {
            log.error("Redis unavailable; successful login counter was not cleared", e);
        }
    }

    private Duration acquireRegistrationAttempt(String clientIp) {
        try {
            return loginRateLimiter.acquireRegistrationAttempt(clientIp);
        } catch (DataAccessException e) {
            log.error("Redis unavailable; registration rate limiting is bypassed", e);
            return Duration.ZERO;
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