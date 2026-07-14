package com.mopl.domain.follow.application.port.out;

import com.mopl.domain.follow.domain.Follow;

public interface DeleteFollowPort {
    // followeeId 기준으로 팔로워 수 캐시를 무효화해야 해서 id만이 아니라 Follow 전체를 받는다
    void delete(Follow follow);
}
