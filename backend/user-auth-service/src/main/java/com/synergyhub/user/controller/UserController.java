package com.synergyhub.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.synergyhub.user.model.User;
import com.synergyhub.user.repository.UserRepository;

@RestController
public class UserController {
    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/users")
    public List<User> getAll() {
        return repo.findAll();
    }

    @PostMapping("/users")
    public User create(@RequestBody User user) {
        return repo.save(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        // This is a mock endpoint for demonstration purposes.
        // In production, use Spring Security for authentication management.
        User found = repo.findByName(user.getName()).orElse(null);
        if (found != null && found.getName().equals(user.getName())) {
            return ResponseEntity.ok("Login successful");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
}
