package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    public static final Set<String> VALID_ROLES = Set.of("SUPER_ADMIN", "DC_ADMIN", "OPERATOR", "VIEWER");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    /**
     * Public self-registration always assigns least-privilege VIEWER role
     * regardless of any client-supplied role parameter.
     */
    public User register(String username, String email, String password, String requestedRole) {
        return createUserWithRole(username, email, password, "VIEWER", "REGISTER", null);
    }

    /**
     * Privileged user creation for SUPER_ADMIN user management.
     */
    public User createUserByAdmin(String username, String email, String password, String role, String actorUsername) {
        String normalizedRole = (role == null || role.isBlank()) ? "OPERATOR" : role.trim().toUpperCase();
        if (!VALID_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        return createUserWithRole(username, email, password, normalizedRole, "CREATE_USER", actorUsername);
    }

    private User createUserWithRole(String username, String email, String password, String assignedRole, String auditAction, String actorUsername) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(trimmedUsername, trimmedEmail, hashedPassword, assignedRole);
        User saved = userRepository.save(user);

        String actor = actorUsername != null ? actorUsername : saved.getUsername();
        Long actorId = actorUsername != null
                ? userRepository.findByUsername(actorUsername).map(User::getId).orElse(saved.getId())
                : saved.getId();

        auditService.log(actorId, actor, auditAction, "USER", saved.getId(), null,
                "Created user '" + saved.getUsername() + "' with role " + saved.getRole());

        return saved;
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new SecurityException("Invalid username or password"));

        if (!user.isActive()) {
            auditService.log(user.getId(), user.getUsername(), "LOGIN_BLOCKED", "USER", user.getId(), null,
                    "Blocked login attempt for disabled account");
            throw new SecurityException("User account is disabled. Contact an administrator.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new SecurityException("Invalid username or password");
        }

        auditService.log(user.getId(), user.getUsername(), "LOGIN", "USER", user.getId(), null, "User logged in successfully");

        return user;
    }

    public String generateToken(User user) {
        return jwtService.generateToken(user.getUsername());
    }
}
