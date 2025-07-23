package com.synergyhub.user.repository;

import com.synergyhub.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}