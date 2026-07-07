package com.mopl.domain.follow.application.service;

import com.mopl.domain.follow.application.port.out.DeleteFollowPort;
import com.mopl.domain.follow.application.port.out.LoadFollowPort;
import com.mopl.domain.follow.application.port.out.LoadUserPort;
import com.mopl.domain.follow.application.port.out.SaveFollowPort;
import com.mopl.domain.follow.domain.Follow;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowService 테스트")
class FollowServiceTest {

    @Mock
    private SaveFollowPort saveFollowPort;

    @Mock
    private LoadFollowPort loadFollowPort;

    @Mock
    private DeleteFollowPort deleteFollowPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private FollowService followService;

    private UUID followerId;
    private UUID followeeId;
    private UUID followId;
    private Follow follow;

    @BeforeEach
    void setUp() {
        followService = new FollowService(saveFollowPort, loadFollowPort, deleteFollowPort,
                loadUserPort, notificationEventPublisher);

        followerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();
        followId = UUID.randomUUID();
        follow = Follow.builder()
                .id(followId)
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("follow: 사용자 팔로우")
    class FollowUser {

        @Test
        @DisplayName("성공: 팔로우하지 않은 사용자를 팔로우한다")
        void follow_success() {
            given(loadFollowPort.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(false);
            given(saveFollowPort.save(any(Follow.class)))
                    .willReturn(follow);
            given(loadUserPort.getUserSummary(followerId))
                    .willReturn(new UserSummary(followerId, "홍길동", null));

            Follow result = followService.follow(followeeId, followerId);

            assertThat(result).isEqualTo(follow);
            assertThat(result.getFolloweeId()).isEqualTo(followeeId);
            assertThat(result.getFollowerId()).isEqualTo(followerId);
            verify(saveFollowPort).save(any(Follow.class));
            verify(notificationEventPublisher).publish(any(NotificationRequestedEvent.class));
        }

        @Test
        @DisplayName("실패: 이미 팔로우한 사용자를 다시 팔로우하면 예외를 던진다")
        void follow_fail_already_exists() {
            given(loadFollowPort.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(true);

            assertThatThrownBy(() -> followService.follow(followeeId, followerId))
                    .isInstanceOf(MoplException.class)
                    .hasMessageContaining(ErrorCode.FOLLOW_ALREADY_EXISTS.getMessage());

            verify(saveFollowPort, never()).save(any(Follow.class));
        }

        @Test
        @DisplayName("실패: 자신을 팔로우하려고 하면 예외를 던진다")
        void follow_fail_self_follow() {
            UUID sameId = UUID.randomUUID();
            given(loadFollowPort.existsByFolloweeIdAndFollowerId(sameId, sameId))
                    .willReturn(false);

            assertThatThrownBy(() -> followService.follow(sameId, sameId))
                    .isInstanceOf(MoplException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_FOLLOW_SELF.getMessage());

            verify(saveFollowPort, never()).save(any(Follow.class));
        }

        @Test
        @DisplayName("성공: 팔로우 저장 후 반환되는 객체가 올바른 필드를 가진다")
        void follow_returns_correct_object() {
            given(loadFollowPort.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(false);
            given(saveFollowPort.save(any(Follow.class)))
                    .willReturn(follow);
            given(loadUserPort.getUserSummary(followerId))
                    .willReturn(new UserSummary(followerId, "홍길동", null));

            Follow result = followService.follow(followeeId, followerId);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("unfollow: 팔로우 취소")
    class Unfollow {

        @Test
        @DisplayName("성공: 자신의 팔로우를 취소한다")
        void unfollow_success() {
            given(loadFollowPort.findById(followId))
                    .willReturn(Optional.of(follow));

            followService.unfollow(followId, followerId);

            verify(deleteFollowPort).deleteById(followId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팔로우를 취소하면 예외를 던진다")
        void unfollow_fail_not_found() {
            given(loadFollowPort.findById(followId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> followService.unfollow(followId, followerId))
                    .isInstanceOf(MoplException.class)
                    .hasMessageContaining(ErrorCode.FOLLOW_NOT_FOUND.getMessage());

            verify(deleteFollowPort, never()).deleteById(any());
        }

        @Test
        @DisplayName("실패: 다른 사용자의 팔로우를 취소하려고 하면 예외를 던진다")
        void unfollow_fail_forbidden() {
            UUID differentUserId = UUID.randomUUID();
            given(loadFollowPort.findById(followId))
                    .willReturn(Optional.of(follow));

            assertThatThrownBy(() -> followService.unfollow(followId, differentUserId))
                    .isInstanceOf(MoplException.class)
                    .hasMessageContaining(ErrorCode.FORBIDDEN.getMessage());

            verify(deleteFollowPort, never()).deleteById(any());
        }

        @Test
        @DisplayName("성공: 팔로우 취소 시 DeleteFollowPort의 deleteById가 정확한 ID로 호출된다")
        void unfollow_calls_delete_with_correct_id() {
            given(loadFollowPort.findById(followId))
                    .willReturn(Optional.of(follow));

            followService.unfollow(followId, followerId);

            verify(deleteFollowPort).deleteById(followId);
        }

        @Test
        @DisplayName("실패: 팔로워가 일치하지 않으면 삭제되지 않는다")
        void unfollow_different_follower_id() {
            UUID differentFollowerId = UUID.randomUUID();
            Follow followWithDifferentFollower = Follow.builder()
                    .id(followId)
                    .followerId(differentFollowerId)
                    .followeeId(followeeId)
                    .createdAt(Instant.now())
                    .build();

            given(loadFollowPort.findById(followId))
                    .willReturn(Optional.of(followWithDifferentFollower));

            assertThatThrownBy(() -> followService.unfollow(followId, followerId))
                    .isInstanceOf(MoplException.class)
                    .hasMessageContaining(ErrorCode.FORBIDDEN.getMessage());

            verify(deleteFollowPort, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getFollowedByMe: 특정 사용자를 팔로우 중인지 조회")
    class GetFollowedByMe {

        @Test
        @DisplayName("성공: 팔로우 관계를 조회한다")
        void getFollowedByMe_success() {
            given(loadFollowPort.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.of(follow));

            Follow result = followService.getFollowedByMe(followeeId, followerId);

            assertThat(result).isEqualTo(follow);
            assertThat(result.getFolloweeId()).isEqualTo(followeeId);
            assertThat(result.getFollowerId()).isEqualTo(followerId);
        }

        @Test
        @DisplayName("실패: 팔로우 관계가 없으면 예외를 던진다")
        void getFollowedByMe_fail_not_found() {
            given(loadFollowPort.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> followService.getFollowedByMe(followeeId, followerId))
                    .isInstanceOf(MoplException.class)
                    .hasMessageContaining(ErrorCode.FOLLOW_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("성공: 조회된 Follow 객체가 올바른 필드를 가진다")
        void getFollowedByMe_returns_complete_object() {
            given(loadFollowPort.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.of(follow));

            Follow result = followService.getFollowedByMe(followeeId, followerId);

            assertThat(result.getId()).isEqualTo(followId);
            assertThat(result.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패: 다른 팔로워가 조회되면 예외를 던진다")
        void getFollowedByMe_different_follower() {
            given(loadFollowPort.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> followService.getFollowedByMe(followeeId, followerId))
                    .isInstanceOf(MoplException.class);
        }
    }

    @Nested
    @DisplayName("getFollowerIds: 팔로워 ID 목록 조회")
    class GetFollowerIds {

        @Test
        @DisplayName("성공: 팔로워 ID 목록을 반환한다")
        void getFollowerIds_success() {
            java.util.List<UUID> followerIds = java.util.List.of(followerId, UUID.randomUUID());
            given(loadFollowPort.findFollowerIdsByFolloweeId(followeeId))
                    .willReturn(followerIds);

            java.util.List<UUID> result = followService.getFollowerIds(followeeId);

            assertThat(result).isEqualTo(followerIds);
        }

        @Test
        @DisplayName("성공: 팔로워가 없으면 빈 목록을 반환한다")
        void getFollowerIds_empty() {
            given(loadFollowPort.findFollowerIdsByFolloweeId(followeeId))
                    .willReturn(java.util.List.of());

            java.util.List<UUID> result = followService.getFollowerIds(followeeId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countFollowers: 팔로워 수 조회")
    class CountFollowers {

        @Test
        @DisplayName("성공: 특정 사용자의 팔로워 수를 조회한다")
        void countFollowers_success() {
            long followerCount = 5L;
            given(loadFollowPort.countByFolloweeId(followeeId))
                    .willReturn(followerCount);

            long result = followService.countFollowers(followeeId);

            assertThat(result).isEqualTo(followerCount);
        }

        @Test
        @DisplayName("성공: 팔로워가 0명인 경우")
        void countFollowers_zero() {
            given(loadFollowPort.countByFolloweeId(followeeId))
                    .willReturn(0L);

            long result = followService.countFollowers(followeeId);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("성공: 많은 팔로워 수를 반환한다")
        void countFollowers_large_count() {
            long largeCount = 10000L;
            given(loadFollowPort.countByFolloweeId(followeeId))
                    .willReturn(largeCount);

            long result = followService.countFollowers(followeeId);

            assertThat(result).isEqualTo(largeCount);
        }

        @Test
        @DisplayName("성공: 여러 번 호출하면 같은 결과를 반환한다")
        void countFollowers_consistent_results() {
            long followerCount = 10L;
            given(loadFollowPort.countByFolloweeId(followeeId))
                    .willReturn(followerCount);

            long result1 = followService.countFollowers(followeeId);
            long result2 = followService.countFollowers(followeeId);

            assertThat(result1).isEqualTo(result2).isEqualTo(followerCount);
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenarios {

        @Test
        @DisplayName("시나리오: A가 B를 팔로우하고, 팔로우 수를 확인하고, 팔로우를 취소한다")
        void scenario_follow_count_unfollow() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            UUID followId = UUID.randomUUID();
            Follow follow = Follow.builder()
                    .id(followId)
                    .followerId(userA)
                    .followeeId(userB)
                    .createdAt(Instant.now())
                    .build();

            // 팔로우
            given(loadFollowPort.existsByFolloweeIdAndFollowerId(userB, userA))
                    .willReturn(false);
            given(saveFollowPort.save(any(Follow.class)))
                    .willReturn(follow);
            given(loadUserPort.getUserSummary(userA))
                    .willReturn(new UserSummary(userA, "홍길동", null));

            Follow followResult = followService.follow(userB, userA);
            assertThat(followResult.getFolloweeId()).isEqualTo(userB);

            // 팔로우 수 확인
            given(loadFollowPort.countByFolloweeId(userB))
                    .willReturn(1L);

            long count = followService.countFollowers(userB);
            assertThat(count).isEqualTo(1L);

            // 팔로우 취소
            given(loadFollowPort.findById(followId))
                    .willReturn(Optional.of(follow));

            followService.unfollow(followId, userA);
            verify(deleteFollowPort).deleteById(followId);
        }
    }
}
