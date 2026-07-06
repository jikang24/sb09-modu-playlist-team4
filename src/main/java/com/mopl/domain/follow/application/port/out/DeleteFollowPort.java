package com.mopl.domain.follow.application.port.out;

import java.util.UUID;

public interface DeleteFollowPort {
    void deleteById(UUID followId);
}
