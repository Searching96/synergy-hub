package com.synergyhub.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.synergyhub.user.model.User;
import com.synergyhub.user.repository.UserRepository;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository repo;
   
    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<User> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return repo.save(user);
    }
}
