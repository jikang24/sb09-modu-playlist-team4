package com.mopl.domain.follow.adapter.out.persistence;

import com.mopl.domain.follow.adapter.out.FollowJpaEntity;
import com.mopl.domain.follow.domain.Follow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowPersistenceAdapter 테스트")
class FollowPersistenceAdapterTest {

    @Mock
    private FollowJpaRepository followJpaRepository;

    @Mock
    private FollowPersistenceMapper mapper;

    private FollowPersistenceAdapter adapter;

    private UUID followId;
    private UUID followerId;
    private UUID followeeId;
    private Follow follow;
    private FollowJpaEntity jpaEntity;

    @BeforeEach
    void setUp() {
        adapter = new FollowPersistenceAdapter(followJpaRepository, mapper);

        followId = UUID.randomUUID();
        followerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();

        follow = Follow.builder()
                .id(followId)
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(Instant.now())
                .build();

        jpaEntity = new FollowJpaEntity(
                followId,
                followeeId,
                followerId,
                Instant.now()
        );
    }

    @Nested
    @DisplayName("save: Follow 저장")
    class Save {

        @Test
        @DisplayName("성공: Follow를 저장하고 반환한다")
        void save_success() {
            given(mapper.toJpaEntity(follow)).willReturn(jpaEntity);
            given(followJpaRepository.save(jpaEntity)).willReturn(jpaEntity);
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Follow result = adapter.save(follow);

            assertThat(result).isEqualTo(follow);
            assertThat(result.getId()).isEqualTo(followId);
            assertThat(result.getFollowerId()).isEqualTo(followerId);
            assertThat(result.getFolloweeId()).isEqualTo(followeeId);
        }

        @Test
        @DisplayName("성공: 저장된 Follow는 모든 필드를 가진다")
        void save_returns_complete_follow() {
            given(mapper.toJpaEntity(follow)).willReturn(jpaEntity);
            given(followJpaRepository.save(jpaEntity)).willReturn(jpaEntity);
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Follow result = adapter.save(follow);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getFollowerId()).isNotNull();
            assertThat(result.getFolloweeId()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공: 매퍼가 올바르게 호출된다")
        void save_calls_mapper_correctly() {
            given(mapper.toJpaEntity(follow)).willReturn(jpaEntity);
            given(followJpaRepository.save(jpaEntity)).willReturn(jpaEntity);
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            adapter.save(follow);

            verify(mapper).toJpaEntity(follow);
            verify(followJpaRepository).save(jpaEntity);
            verify(mapper).toDomain(jpaEntity);
        }
    }

    @Nested
    @DisplayName("findById: ID로 Follow 조회")
    class FindById {

        @Test
        @DisplayName("성공: ID로 Follow를 조회한다")
        void findById_success() {
            given(followJpaRepository.findById(followId))
                    .willReturn(Optional.of(jpaEntity));
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Optional<Follow> result = adapter.findById(followId);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(follow);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
        void findById_not_found() {
            given(followJpaRepository.findById(followId))
                    .willReturn(Optional.empty());

            Optional<Follow> result = adapter.findById(followId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 조회된 Follow의 필드가 정확하다")
        void findById_returns_correct_object() {
            given(followJpaRepository.findById(followId))
                    .willReturn(Optional.of(jpaEntity));
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Optional<Follow> result = adapter.findById(followId);

            assertThat(result.get().getId()).isEqualTo(followId);
            assertThat(result.get().getFollowerId()).isEqualTo(followerId);
            assertThat(result.get().getFolloweeId()).isEqualTo(followeeId);
        }

        @Test
        @DisplayName("성공: 리포지토리가 올바른 ID로 호출된다")
        void findById_calls_repository_with_correct_id() {
            given(followJpaRepository.findById(followId))
                    .willReturn(Optional.empty());

            adapter.findById(followId);

            verify(followJpaRepository).findById(followId);
        }
    }

    @Nested
    @DisplayName("findByFolloweeIdAndFollowerId: 팔로워와 팔로이 ID로 Follow 조회")
    class FindByFolloweeIdAndFollowerId {

        @Test
        @DisplayName("성공: 팔로워와 팔로이 ID로 Follow를 조회한다")
        void findByFolloweeIdAndFollowerId_success() {
            given(followJpaRepository.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.of(jpaEntity));
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Optional<Follow> result = adapter.findByFolloweeIdAndFollowerId(followeeId, followerId);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(follow);
        }

        @Test
        @DisplayName("실패: 팔로우 관계가 없으면 빈 Optional을 반환한다")
        void findByFolloweeIdAndFollowerId_not_found() {
            given(followJpaRepository.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.empty());

            Optional<Follow> result = adapter.findByFolloweeIdAndFollowerId(followeeId, followerId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 조회된 Follow의 필드가 정확하다")
        void findByFolloweeIdAndFollowerId_returns_correct_object() {
            given(followJpaRepository.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.of(jpaEntity));
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Optional<Follow> result = adapter.findByFolloweeIdAndFollowerId(followeeId, followerId);

            assertThat(result.get().getFolloweeId()).isEqualTo(followeeId);
            assertThat(result.get().getFollowerId()).isEqualTo(followerId);
        }

        @Test
        @DisplayName("성공: 리포지토리가 올바른 파라미터로 호출된다")
        void findByFolloweeIdAndFollowerId_calls_repository_with_correct_params() {
            given(followJpaRepository.findByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(Optional.empty());

            adapter.findByFolloweeIdAndFollowerId(followeeId, followerId);

            verify(followJpaRepository).findByFolloweeIdAndFollowerId(followeeId, followerId);
        }
    }

    @Nested
    @DisplayName("existsByFolloweeIdAndFollowerId: 팔로우 관계 존재 확인")
    class ExistsByFolloweeIdAndFollowerId {

        @Test
        @DisplayName("성공: 팔로우 관계가 존재하면 true를 반환한다")
        void existsByFolloweeIdAndFollowerId_exists() {
            given(followJpaRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(true);

            boolean result = adapter.existsByFolloweeIdAndFollowerId(followeeId, followerId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공: 팔로우 관계가 없으면 false를 반환한다")
        void existsByFolloweeIdAndFollowerId_not_exists() {
            given(followJpaRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(false);

            boolean result = adapter.existsByFolloweeIdAndFollowerId(followeeId, followerId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("성공: 리포지토리가 올바른 파라미터로 호출된다")
        void existsByFolloweeIdAndFollowerId_calls_repository_with_correct_params() {
            given(followJpaRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(false);

            adapter.existsByFolloweeIdAndFollowerId(followeeId, followerId);

            verify(followJpaRepository).existsByFolloweeIdAndFollowerId(followeeId, followerId);
        }

        @Test
        @DisplayName("성공: 팔로워와 팔로이 순서가 중요하다")
        void existsByFolloweeIdAndFollowerId_order_matters() {
            UUID differentFolloweeId = UUID.randomUUID();
            given(followJpaRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(true);
            given(followJpaRepository.existsByFolloweeIdAndFollowerId(differentFolloweeId, followerId))
                    .willReturn(false);

            boolean result1 = adapter.existsByFolloweeIdAndFollowerId(followeeId, followerId);
            boolean result2 = adapter.existsByFolloweeIdAndFollowerId(differentFolloweeId, followerId);

            assertThat(result1).isTrue();
            assertThat(result2).isFalse();
        }
    }

    @Nested
    @DisplayName("countByFolloweeId: 팔로워 수 조회")
    class CountByFolloweeId {

        @Test
        @DisplayName("성공: 특정 팔로이의 팔로워 수를 반환한다")
        void countByFolloweeId_success() {
            long count = 5L;
            given(followJpaRepository.countByFolloweeId(followeeId))
                    .willReturn(count);

            long result = adapter.countByFolloweeId(followeeId);

            assertThat(result).isEqualTo(count);
        }

        @Test
        @DisplayName("성공: 팔로워가 없으면 0을 반환한다")
        void countByFolloweeId_zero() {
            given(followJpaRepository.countByFolloweeId(followeeId))
                    .willReturn(0L);

            long result = adapter.countByFolloweeId(followeeId);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("성공: 많은 팔로워 수를 반환한다")
        void countByFolloweeId_large_count() {
            long largeCount = 100000L;
            given(followJpaRepository.countByFolloweeId(followeeId))
                    .willReturn(largeCount);

            long result = adapter.countByFolloweeId(followeeId);

            assertThat(result).isEqualTo(largeCount);
        }

        @Test
        @DisplayName("성공: 리포지토리가 올바른 ID로 호출된다")
        void countByFolloweeId_calls_repository_with_correct_id() {
            given(followJpaRepository.countByFolloweeId(followeeId))
                    .willReturn(0L);

            adapter.countByFolloweeId(followeeId);

            verify(followJpaRepository).countByFolloweeId(followeeId);
        }

        @Test
        @DisplayName("성공: 다른 팔로이의 팔로워 수는 다르다")
        void countByFolloweeId_different_followees() {
            UUID anotherFolloweeId = UUID.randomUUID();
            given(followJpaRepository.countByFolloweeId(followeeId))
                    .willReturn(10L);
            given(followJpaRepository.countByFolloweeId(anotherFolloweeId))
                    .willReturn(20L);

            long result1 = adapter.countByFolloweeId(followeeId);
            long result2 = adapter.countByFolloweeId(anotherFolloweeId);

            assertThat(result1).isEqualTo(10L);
            assertThat(result2).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("findFollowerIdsByFolloweeId: 팔로워 ID 목록 조회")
    class FindFollowerIdsByFolloweeId {

        @Test
        @DisplayName("성공: 팔로워 ID 목록을 반환한다")
        void findFollowerIdsByFolloweeId_success() {
            List<UUID> followerIds = List.of(followerId, UUID.randomUUID());
            given(followJpaRepository.findFollowerIdsByFolloweeId(followeeId))
                    .willReturn(followerIds);

            List<UUID> result = adapter.findFollowerIdsByFolloweeId(followeeId);

            assertThat(result).isEqualTo(followerIds);
        }

        @Test
        @DisplayName("성공: 팔로워가 없으면 빈 목록을 반환한다")
        void findFollowerIdsByFolloweeId_empty() {
            given(followJpaRepository.findFollowerIdsByFolloweeId(followeeId))
                    .willReturn(List.of());

            List<UUID> result = adapter.findFollowerIdsByFolloweeId(followeeId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 리포지토리가 올바른 ID로 호출된다")
        void findFollowerIdsByFolloweeId_calls_repository_with_correct_id() {
            given(followJpaRepository.findFollowerIdsByFolloweeId(followeeId))
                    .willReturn(List.of());

            adapter.findFollowerIdsByFolloweeId(followeeId);

            verify(followJpaRepository).findFollowerIdsByFolloweeId(followeeId);
        }
    }

    @Nested
    @DisplayName("delete: Follow 삭제")
    class Delete {

        @Test
        @DisplayName("성공: Follow를 삭제한다")
        void delete_success() {
            adapter.delete(follow);

            verify(followJpaRepository).deleteById(followId);
        }

        @Test
        @DisplayName("성공: 올바른 ID로 삭제가 호출된다")
        void delete_calls_repository_with_correct_id() {
            UUID differentId = UUID.randomUUID();
            Follow differentFollow = Follow.builder()
                    .id(differentId)
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .createdAt(Instant.now())
                    .build();

            adapter.delete(differentFollow);

            verify(followJpaRepository).deleteById(differentId);
        }

        @Test
        @DisplayName("성공: 여러 번의 삭제 호출이 가능하다")
        void delete_multiple_calls() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Follow follow1 = Follow.builder()
                    .id(id1).followerId(followerId).followeeId(followeeId).createdAt(Instant.now()).build();
            Follow follow2 = Follow.builder()
                    .id(id2).followerId(followerId).followeeId(followeeId).createdAt(Instant.now()).build();

            adapter.delete(follow1);
            adapter.delete(follow2);

            verify(followJpaRepository).deleteById(id1);
            verify(followJpaRepository).deleteById(id2);
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenarios {

        @Test
        @DisplayName("시나리오: Follow를 저장하고 조회한다")
        void scenario_save_and_find() {
            given(mapper.toJpaEntity(follow)).willReturn(jpaEntity);
            given(followJpaRepository.save(jpaEntity)).willReturn(jpaEntity);
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Follow saved = adapter.save(follow);

            given(followJpaRepository.findById(followId))
                    .willReturn(Optional.of(jpaEntity));
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            Optional<Follow> found = adapter.findById(followId);

            assertThat(found).isPresent();
            assertThat(found.get()).isEqualTo(saved);
        }

        @Test
        @DisplayName("시나리오: Follow를 저장하고 존재 여부를 확인하고 삭제한다")
        void scenario_save_exists_delete() {
            given(mapper.toJpaEntity(follow)).willReturn(jpaEntity);
            given(followJpaRepository.save(jpaEntity)).willReturn(jpaEntity);
            given(mapper.toDomain(jpaEntity)).willReturn(follow);

            adapter.save(follow);

            given(followJpaRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId))
                    .willReturn(true);

            boolean exists = adapter.existsByFolloweeIdAndFollowerId(followeeId, followerId);
            assertThat(exists).isTrue();

            adapter.delete(follow);
            verify(followJpaRepository).deleteById(followId);
        }
    }
}
