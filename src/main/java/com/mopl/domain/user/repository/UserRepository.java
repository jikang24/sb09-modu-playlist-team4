package com.mopl.domain.user.repository;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom  {
    boolean existsByEmail(String email);

    boolean existsByName(String name);

    boolean existsByRole(Role role);
}
