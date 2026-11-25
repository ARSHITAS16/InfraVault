package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.DatacenterUser;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.DatacenterUserRepository;
import com.passwordmanager.backend.repository.UserRepository;
import com.passwordmanager.backend.service.AuditService;
import com.passwordmanager.backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final DatacenterUserRepository datacenterUserRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public UserController(UserRepository userRepository,
                          DatacenterUserRepository datacenterUserRepository,
                          AuthService authService,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.datacenterUserRepository = datacenterUserRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(Authentication authentication) {
        User actor = getActor(authentication);
        if (!"SUPER_ADMIN".equalsIgnoreCase(actor.getRole()) && !"DC_ADMIN".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Permission denied: Only administrators can view system users");
        }

        List<Map<String, Object>> result = userRepository.findAll().stream()
                .map(this::toSafeUserMap)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody CreateUserRequest request,
                                                          Authentication authentication) {
        User actor = requireSuperAdmin(authentication);
        User created = authService.createUserByAdmin(
                request.username(),
                request.email(),
                request.password(),
                request.role(),
                actor.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toSafeUserMap(created));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Transactional
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id,
                                                          @RequestBody UpdateUserRequest request,
                                                          Authentication authentication) {
        User actor = requireSuperAdmin(authentication);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.role() != null && !request.role().isBlank()) {
            String normalizedRole = request.role().trim().toUpperCase();
            if (!AuthService.VALID_ROLES.contains(normalizedRole)) {
                throw new IllegalArgumentException("Invalid role: " + request.role());
            }
            if (actor.getId().equals(target.getId()) && !"SUPER_ADMIN".equals(normalizedRole)) {
                throw new IllegalStateException("Cannot demote your own SUPER_ADMIN account");
            }
            target.setRole(normalizedRole);
        }

        if (request.active() != null) {
            if (actor.getId().equals(target.getId()) && !request.active()) {
                throw new IllegalStateException("Cannot disable your own administrator account");
            }
            target.setActive(request.active());
        }

        if (request.email() != null && !request.email().isBlank()
                && !request.email().trim().equalsIgnoreCase(target.getEmail())) {
            if (userRepository.existsByEmail(request.email().trim())) {
                throw new IllegalArgumentException("Email already exists");
            }
            target.setEmail(request.email().trim());
        }

        User saved = userRepository.save(target);
        auditService.log(actor.getId(), actor.getUsername(), "UPDATE_USER", "USER", saved.getId(), null,
                "Updated user '" + saved.getUsername() + "' (role=" + saved.getRole() + ", active=" + saved.isActive() + ")");

        return ResponseEntity.ok(toSafeUserMap(saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        User actor = requireSuperAdmin(authentication);
        if (actor.getId().equals(id)) {
            throw new IllegalStateException("Cannot delete your own administrator account");
        }
        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<DatacenterUser> assignments = datacenterUserRepository.findByUserId(id);
        datacenterUserRepository.deleteAll(assignments);
        userRepository.delete(target);

        auditService.log(actor.getId(), actor.getUsername(), "DELETE_USER", "USER", id, null,
                "Deleted user '" + target.getUsername() + "'");

        return ResponseEntity.noContent().build();
    }

    private User requireSuperAdmin(Authentication authentication) {
        User actor = getActor(authentication);
        if (!"SUPER_ADMIN".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Permission denied: SUPER_ADMIN role required");
        }
        return actor;
    }

    private User getActor(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private Map<String, Object> toSafeUserMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("email", u.getEmail());
        map.put("role", u.getRole());
        map.put("active", u.isActive());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }

    public record CreateUserRequest(String username, String email, String password, String role) {}

    public record UpdateUserRequest(String email, String role, Boolean active) {}
}
