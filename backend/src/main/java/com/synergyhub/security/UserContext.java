package com.synergyhub.security;

import java.util.Set;

public class UserContext {
    private final Integer id;
    private final String email;
    private final Set<String> roles;

    public UserContext(Integer id, String email, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.roles = roles;
    }

    public Integer getId() { return id; }
    public String getEmail() { return email; }
    public Set<String> getRoles() { return roles; }
}
