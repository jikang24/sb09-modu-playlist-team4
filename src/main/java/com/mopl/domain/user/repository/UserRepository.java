package com.mopl.domain.user.repository;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom  {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByName(String name);

    boolean existsByRole(Role role);

}
