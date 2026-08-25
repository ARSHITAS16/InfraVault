package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Hide password hash for safety
        users.forEach(u -> u.setPasswordHash(null));
        return ResponseEntity.ok(users);
    }
}
