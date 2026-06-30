package com.mopl.domain.user.service;

import com.mopl.global.dto.Role;
import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .name("admin")
                .email("system@mopl.io")
                .password(passwordEncoder.encode("admin1!"))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
    }
}