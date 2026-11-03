package com.Author.Fitness.User;

import com.Author.Fitness.Auth.dto.AuthResponse;
import com.Author.Fitness.Security.ClientIpResolver;
import com.Author.Fitness.Security.CustomUserDetails;
import com.Author.Fitness.User.dto.ChangePasswordRequest;
import com.Author.Fitness.User.dto.CreateUserRequest;
import com.Author.Fitness.User.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService service;
    private final PasswordService passwordService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/{id}")
    public UserResponse getUserById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        return service.getUserById(id, currentUser);
    }

    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request){
        return service.createUser(request);
    }

    @PatchMapping("/me/password")
    public AuthResponse changePassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        return passwordService.changePassword(currentUser.getId(), request, clientIp);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ){
        service.deleteUser(id, currentUser);
    }
}
