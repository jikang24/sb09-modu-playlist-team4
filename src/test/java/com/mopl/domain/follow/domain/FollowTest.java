package com.mopl.domain.follow.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Follow 도메인 엔티티 테스트")
class FollowTest {

    @Nested
    @DisplayName("create: Follow 생성")
    class Create {

        @Test
        @DisplayName("성공: 다른 사용자를 팔로우한다")
        void create_success() {
            UUID followeeId = UUID.randomUUID();
            UUID followerId = UUID.randomUUID();

            Follow follow = Follow.create(followeeId, followerId);

            assertThat(follow).isNotNull();
            assertThat(follow.getId()).isNotNull();
            assertThat(follow.getFollowerId()).isEqualTo(followerId);
            assertThat(follow.getFolloweeId()).isEqualTo(followeeId);
            assertThat(follow.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패: 자신을 팔로우하려고 하면 예외를 던진다")
        void create_fail_self_follow() {
            UUID sameId = UUID.randomUUID();

            assertThatThrownBy(() -> Follow.create(sameId, sameId))
                    .isInstanceOf(MoplException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_FOLLOW_SELF.getMessage());
        }

        @Test
        @DisplayName("생성된 Follow는 고유한 ID를 가진다")
        void create_unique_ids() {
            UUID followeeId = UUID.randomUUID();
            UUID followerId = UUID.randomUUID();

            Follow follow1 = Follow.create(followeeId, followerId);
            Follow follow2 = Follow.create(followeeId, followerId);

            assertThat(follow1.getId()).isNotEqualTo(follow2.getId());
        }

        @Test
        @DisplayName("생성된 Follow의 타임스탬프는 현재 시간이다")
        void create_timestamp_is_now() {
            UUID followeeId = UUID.randomUUID();
            UUID followerId = UUID.randomUUID();
            long beforeCreation = System.currentTimeMillis();

            Follow follow = Follow.create(followeeId, followerId);

            long afterCreation = System.currentTimeMillis();
            long createdAtMillis = follow.getCreatedAt().toEpochMilli();

            assertThat(createdAtMillis).isBetween(beforeCreation, afterCreation);
        }
    }
}
