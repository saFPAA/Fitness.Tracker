package com.Author.Fitness.Auth;

import com.Author.Fitness.Auth.dto.AuthResponse;
import com.Author.Fitness.Auth.dto.LoginRequest;
import com.Author.Fitness.Auth.dto.RefreshRequest;
import com.Author.Fitness.Security.ClientIpResolver;
import com.Author.Fitness.User.dto.CreateUserRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final ClientIpResolver clientIpResolver;
    private final AuthService service;

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        return service.register(request, clientIp);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        return service.login(request, clientIp);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request){
        return service.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest request){
        service.logout(request);
    }
}
