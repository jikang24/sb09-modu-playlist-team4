package com.mopl.domain.follow.application.port.out;

import com.mopl.domain.follow.domain.Follow;

public interface SaveFollowPort {
    Follow save(Follow follow);
}
