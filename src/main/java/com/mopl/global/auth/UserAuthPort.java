package com.mopl.global.auth;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthPort {
    Optional<UserAuthInfo> findByEmail(String email);

    Optional<UserAuthInfo> findById(UUID userId);
}
