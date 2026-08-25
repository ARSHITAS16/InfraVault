package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.dto.AuthResponse;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        User user = authService.register(
                request.username(),
                request.email(),
                request.password(),
                request.role()
        );

        String token = authService.generateToken(user);

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        User user = authService.login(
                request.username(),
                request.password()
        );

        String token = authService.generateToken(user);

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }

    public record RegisterRequest(
            String username,
            String email,
            String password,
            String role
    ) {}

    public record LoginRequest(
            String username,
            String password
    ) {}
}
