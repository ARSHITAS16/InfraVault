package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.dto.AuthResponse;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = authService.register(
                request.username(),
                request.email(),
                request.password(),
                request.role()
        );

        String token = authService.generateToken(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = authService.login(
                request.username(),
                request.password()
        );

        String token = authService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        ));
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
