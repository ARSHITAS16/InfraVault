package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public User register(String username, String email, String password, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, email, hashedPassword, role);
        User saved = userRepository.save(user);

        auditService.log(saved.getId(), saved.getUsername(), "REGISTER", "USER", saved.getId(), null, "User registered with role " + saved.getRole());

        return saved;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password");
        }

        auditService.log(user.getId(), user.getUsername(), "LOGIN", "USER", user.getId(), null, "User logged in successfully");

        return user;
    }

    public String generateToken(User user) {
        return jwtService.generateToken(user.getUsername());
    }
}
