package com.mopl.domain.user.repository;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.UserSearchRequest;

import java.util.List;

public interface UserRepositoryCustom {
    List<User> findAllWithCursor(UserSearchRequest request);
    long countAll(UserSearchRequest request);
}
