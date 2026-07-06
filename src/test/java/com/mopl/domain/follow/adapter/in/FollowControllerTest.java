package com.mopl.domain.follow.adapter.in;

import com.mopl.domain.follow.application.port.in.FollowUserUseCase;
import com.mopl.domain.follow.application.port.in.GetFollowedByMeUseCase;
import com.mopl.domain.follow.application.port.in.GetFollowerCountUseCase;
import com.mopl.domain.follow.application.port.in.UnfollowUserUseCase;
import com.mopl.domain.follow.domain.Follow;
import com.mopl.domain.follow.dto.FollowRequest;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowController 테스트")
class FollowControllerTest {

    @Mock
    private FollowUserUseCase followUserUseCase;

    @Mock
    private UnfollowUserUseCase unfollowUserUseCase;

    @Mock
    private GetFollowedByMeUseCase getFollowedByMeUseCase;

    @Mock
    private GetFollowerCountUseCase getFollowerCountUseCase;

    @Mock
    private JwtClaims jwtClaims;

    private FollowController controller;

    private UUID followerId;
    private UUID followeeId;
    private UUID followId;
    private Follow follow;

    @BeforeEach
    void setUp() {
        controller = new FollowController(
                followUserUseCase,
                unfollowUserUseCase,
                getFollowedByMeUseCase,
                getFollowerCountUseCase
        );

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
    @DisplayName("follow: 팔로우 API")
    class FollowApi {

        @Test
        @DisplayName("성공: 팔로우 요청이 처리되고 200 상태 코드를 반환한다")
        void follow_success() {
            FollowRequest request = new FollowRequest(followeeId);
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(followUserUseCase.follow(followeeId, followerId)).willReturn(follow);

            ResponseEntity<?> response = controller.follow(request, jwtClaims);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(followUserUseCase).follow(followeeId, followerId);
        }

        @Test
        @DisplayName("성공: 응답 바디에 FollowDto가 포함된다")
        void follow_returns_follow_dto() {
            FollowRequest request = new FollowRequest(followeeId);
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(followUserUseCase.follow(followeeId, followerId)).willReturn(follow);

            ResponseEntity<?> response = controller.follow(request, jwtClaims);

            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("실패: 이미 팔로우한 경우 예외가 발생한다")
        void follow_fail_already_exists() {
            FollowRequest request = new FollowRequest(followeeId);
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(followUserUseCase.follow(followeeId, followerId))
                    .willThrow(new MoplException(ErrorCode.FOLLOW_ALREADY_EXISTS));

            try {
                controller.follow(request, jwtClaims);
            } catch (MoplException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_ALREADY_EXISTS);
            }
        }

        @Test
        @DisplayName("실패: 자신을 팔로우하려는 경우 예외가 발생한다")
        void follow_fail_self_follow() {
            FollowRequest request = new FollowRequest(followerId);
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(followUserUseCase.follow(followerId, followerId))
                    .willThrow(new MoplException(ErrorCode.CANNOT_FOLLOW_SELF));

            try {
                controller.follow(request, jwtClaims);
            } catch (MoplException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FOLLOW_SELF);
            }
        }

        @Test
        @DisplayName("성공: JWT 클레임에서 사용자 ID를 올바르게 추출한다")
        void follow_extracts_user_id_from_jwt() {
            FollowRequest request = new FollowRequest(followeeId);
            UUID anotherFollowerId = UUID.randomUUID();
            given(jwtClaims.getUserId()).willReturn(anotherFollowerId);
            given(followUserUseCase.follow(followeeId, anotherFollowerId)).willReturn(follow);

            controller.follow(request, jwtClaims);

            verify(jwtClaims).getUserId();
        }
    }

    @Nested
    @DisplayName("unfollow: 팔로우 취소 API")
    class Unfollow {

        @Test
        @DisplayName("성공: 팔로우 취소 요청이 처리되고 204 상태 코드를 반환한다")
        void unfollow_success() {
            given(jwtClaims.getUserId()).willReturn(followerId);

            ResponseEntity<Void> response = controller.unfollow(followId, jwtClaims);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(unfollowUserUseCase).unfollow(followId, followerId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팔로우를 취소하면 예외가 발생한다")
        void unfollow_fail_not_found() {
            given(jwtClaims.getUserId()).willReturn(followerId);
            doThrow(new MoplException(ErrorCode.FOLLOW_NOT_FOUND))
                    .when(unfollowUserUseCase).unfollow(followId, followerId);

            try {
                controller.unfollow(followId, jwtClaims);
            } catch (MoplException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("실패: 다른 사용자의 팔로우를 취소하려고 하면 예외가 발생한다")
        void unfollow_fail_forbidden() {
            UUID differentUserId = UUID.randomUUID();
            given(jwtClaims.getUserId()).willReturn(differentUserId);
            doThrow(new MoplException(ErrorCode.FORBIDDEN))
                    .when(unfollowUserUseCase).unfollow(followId, differentUserId);

            try {
                controller.unfollow(followId, jwtClaims);
            } catch (MoplException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("성공: 응답 바디가 비어있다")
        void unfollow_response_body_is_empty() {
            given(jwtClaims.getUserId()).willReturn(followerId);

            ResponseEntity<Void> response = controller.unfollow(followId, jwtClaims);

            assertThat(response.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("getFollowedByMe: 팔로우 여부 조회 API")
    class GetFollowedByMe {

        @Test
        @DisplayName("성공: 팔로우 여부 조회 요청이 처리되고 200 상태 코드를 반환한다")
        void getFollowedByMe_success() {
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(getFollowedByMeUseCase.getFollowedByMe(followeeId, followerId))
                    .willReturn(follow);

            ResponseEntity<?> response = controller.getFollowedByMe(followeeId, jwtClaims);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(getFollowedByMeUseCase).getFollowedByMe(followeeId, followerId);
        }

        @Test
        @DisplayName("실패: 팔로우하지 않은 사용자를 조회하면 404 예외가 발생한다")
        void getFollowedByMe_fail_not_found() {
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(getFollowedByMeUseCase.getFollowedByMe(followeeId, followerId))
                    .willThrow(new MoplException(ErrorCode.FOLLOW_NOT_FOUND));

            try {
                controller.getFollowedByMe(followeeId, jwtClaims);
            } catch (MoplException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("성공: 응답 바디에 FollowDto가 포함된다")
        void getFollowedByMe_returns_follow_dto() {
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(getFollowedByMeUseCase.getFollowedByMe(followeeId, followerId))
                    .willReturn(follow);

            ResponseEntity<?> response = controller.getFollowedByMe(followeeId, jwtClaims);

            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("성공: 쿼리 파라미터의 팔로이 ID를 올바르게 처리한다")
        void getFollowedByMe_processes_query_parameter() {
            UUID anotherFolloweeId = UUID.randomUUID();
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(getFollowedByMeUseCase.getFollowedByMe(anotherFolloweeId, followerId))
                    .willReturn(follow);

            controller.getFollowedByMe(anotherFolloweeId, jwtClaims);

            verify(getFollowedByMeUseCase).getFollowedByMe(anotherFolloweeId, followerId);
        }
    }

    @Nested
    @DisplayName("countFollowers: 팔로워 수 조회 API")
    class CountFollowers {

        @Test
        @DisplayName("성공: 팔로워 수 조회 요청이 처리되고 200 상태 코드를 반환한다")
        void countFollowers_success() {
            long count = 10L;
            given(getFollowerCountUseCase.countFollowers(followeeId)).willReturn(count);

            ResponseEntity<Long> response = controller.countFollowers(followeeId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(count);
            verify(getFollowerCountUseCase).countFollowers(followeeId);
        }

        @Test
        @DisplayName("성공: 팔로워가 0명인 경우 0을 반환한다")
        void countFollowers_zero() {
            given(getFollowerCountUseCase.countFollowers(followeeId)).willReturn(0L);

            ResponseEntity<Long> response = controller.countFollowers(followeeId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isZero();
        }

        @Test
        @DisplayName("성공: 많은 팔로워 수를 반환한다")
        void countFollowers_large_count() {
            long largeCount = 100000L;
            given(getFollowerCountUseCase.countFollowers(followeeId)).willReturn(largeCount);

            ResponseEntity<Long> response = controller.countFollowers(followeeId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(largeCount);
        }

        @Test
        @DisplayName("성공: 응답 바디에 팔로워 수(Long)가 포함된다")
        void countFollowers_response_body_is_count() {
            long expectedCount = 25L;
            given(getFollowerCountUseCase.countFollowers(followeeId))
                    .willReturn(expectedCount);

            ResponseEntity<Long> response = controller.countFollowers(followeeId);

            assertThat(response.getBody()).isEqualTo(expectedCount);
        }

        @Test
        @DisplayName("성공: 쿼리 파라미터의 팔로이 ID를 올바르게 처리한다")
        void countFollowers_processes_query_parameter() {
            UUID anotherFolloweeId = UUID.randomUUID();
            given(getFollowerCountUseCase.countFollowers(anotherFolloweeId))
                    .willReturn(5L);

            controller.countFollowers(anotherFolloweeId);

            verify(getFollowerCountUseCase).countFollowers(anotherFolloweeId);
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenarios {

        @Test
        @DisplayName("시나리오: 팔로우 -> 팔로워 수 조회 -> 팔로우 취소")
        void scenario_full_follow_workflow() {
            // 1. 팔로우
            FollowRequest followRequest = new FollowRequest(followeeId);
            given(jwtClaims.getUserId()).willReturn(followerId);
            given(followUserUseCase.follow(followeeId, followerId)).willReturn(follow);

            ResponseEntity<?> followResponse = controller.follow(followRequest, jwtClaims);
            assertThat(followResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // 2. 팔로워 수 조회
            given(getFollowerCountUseCase.countFollowers(followeeId)).willReturn(1L);
            ResponseEntity<Long> countResponse = controller.countFollowers(followeeId);
            assertThat(countResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(countResponse.getBody()).isEqualTo(1L);

            // 3. 팔로우 취소
            ResponseEntity<Void> unfollowResponse = controller.unfollow(followId, jwtClaims);
            assertThat(unfollowResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            verify(followUserUseCase).follow(followeeId, followerId);
            verify(getFollowerCountUseCase).countFollowers(followeeId);
            verify(unfollowUserUseCase).unfollow(followId, followerId);
        }
    }
}
