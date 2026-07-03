package com.mopl.domain.follow.adapter.out.persistence;

import com.mopl.domain.follow.adapter.out.FollowJpaEntity;
import com.mopl.domain.follow.domain.Follow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FollowPersistenceMapper 테스트")
class FollowPersistenceMapperTest {

    private FollowPersistenceMapper mapper;
    private UUID followId;
    private UUID followerId;
    private UUID followeeId;
    private Instant createdAt;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(FollowPersistenceMapper.class);
        followId = UUID.randomUUID();
        followerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();
        createdAt = Instant.now();
    }

    @Nested
    @DisplayName("toJpaEntity: Domain을 JPA Entity로 변환")
    class ToJpaEntity {

        @Test
        @DisplayName("성공: Follow를 FollowJpaEntity로 변환한다")
        void toJpaEntity_success() {
            Follow follow = Follow.builder()
                    .id(followId)
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .createdAt(createdAt)
                    .build();

            FollowJpaEntity entity = mapper.toJpaEntity(follow);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(followId);
            assertThat(entity.getFollowerId()).isEqualTo(followerId);
            assertThat(entity.getFolloweeId()).isEqualTo(followeeId);
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("성공: 모든 필드가 올바르게 매핑된다")
        void toJpaEntity_all_fields_mapped() {
            Follow follow = Follow.builder()
                    .id(followId)
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .createdAt(createdAt)
                    .build();

            FollowJpaEntity entity = mapper.toJpaEntity(follow);

            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getFollowerId()).isNotNull();
            assertThat(entity.getFolloweeId()).isNotNull();
            assertThat(entity.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공: 매핑 후 ID가 변경되지 않는다")
        void toJpaEntity_preserves_id() {
            Follow follow = Follow.builder()
                    .id(followId)
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .createdAt(createdAt)
                    .build();

            FollowJpaEntity entity = mapper.toJpaEntity(follow);

            assertThat(entity.getId()).isEqualTo(follow.getId());
        }

        @Test
        @DisplayName("성공: 팔로워와 팔로이 ID가 올바르게 매핑된다")
        void toJpaEntity_follower_followee_ids() {
            Follow follow = Follow.builder()
                    .id(followId)
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .createdAt(createdAt)
                    .build();

            FollowJpaEntity entity = mapper.toJpaEntity(follow);

            assertThat(entity.getFollowerId()).isEqualTo(followerId);
            assertThat(entity.getFolloweeId()).isEqualTo(followeeId);
            assertThat(entity.getFollowerId()).isNotEqualTo(entity.getFolloweeId());
        }
    }

    @Nested
    @DisplayName("toDomain: JPA Entity를 Domain으로 변환")
    class ToDomain {

        @Test
        @DisplayName("성공: FollowJpaEntity를 Follow로 변환한다")
        void toDomain_success() {
            FollowJpaEntity entity = new FollowJpaEntity(
                    followId,
                    followeeId,
                    followerId,
                    createdAt
            );

            Follow follow = mapper.toDomain(entity);

            assertThat(follow).isNotNull();
            assertThat(follow.getId()).isEqualTo(followId);
            assertThat(follow.getFollowerId()).isEqualTo(followerId);
            assertThat(follow.getFolloweeId()).isEqualTo(followeeId);
            assertThat(follow.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("성공: 모든 필드가 올바르게 매핑된다")
        void toDomain_all_fields_mapped() {
            FollowJpaEntity entity = new FollowJpaEntity(
                    followId,
                    followeeId,
                    followerId,
                    createdAt
            );

            Follow follow = mapper.toDomain(entity);

            assertThat(follow.getId()).isNotNull();
            assertThat(follow.getFollowerId()).isNotNull();
            assertThat(follow.getFolloweeId()).isNotNull();
            assertThat(follow.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공: 매핑 후 ID가 변경되지 않는다")
        void toDomain_preserves_id() {
            FollowJpaEntity entity = new FollowJpaEntity(
                    followId,
                    followeeId,
                    followerId,
                    createdAt
            );

            Follow follow = mapper.toDomain(entity);

            assertThat(follow.getId()).isEqualTo(entity.getId());
        }

        @Test
        @DisplayName("성공: 팔로워와 팔로이 ID가 올바르게 매핑된다")
        void toDomain_follower_followee_ids() {
            FollowJpaEntity entity = new FollowJpaEntity(
                    followId,
                    followeeId,
                    followerId,
                    createdAt
            );

            Follow follow = mapper.toDomain(entity);

            assertThat(follow.getFollowerId()).isEqualTo(followerId);
            assertThat(follow.getFolloweeId()).isEqualTo(followeeId);
            assertThat(follow.getFollowerId()).isNotEqualTo(follow.getFolloweeId());
        }
    }

    @Nested
    @DisplayName("양방향 매핑 일관성")
    class BidirectionalConsistency {

        @Test
        @DisplayName("성공: Follow -> Entity -> Follow 매핑이 일관성 있다")
        void bidirectional_follow_to_entity_to_follow() {
            Follow originalFollow = Follow.builder()
                    .id(followId)
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .createdAt(createdAt)
                    .build();

            FollowJpaEntity entity = mapper.toJpaEntity(originalFollow);
            Follow mappedFollow = mapper.toDomain(entity);

            assertThat(mappedFollow.getId()).isEqualTo(originalFollow.getId());
            assertThat(mappedFollow.getFollowerId()).isEqualTo(originalFollow.getFollowerId());
            assertThat(mappedFollow.getFolloweeId()).isEqualTo(originalFollow.getFolloweeId());
            assertThat(mappedFollow.getCreatedAt()).isEqualTo(originalFollow.getCreatedAt());
        }

        @Test
        @DisplayName("성공: Entity -> Follow -> Entity 매핑이 일관성 있다")
        void bidirectional_entity_to_follow_to_entity() {
            FollowJpaEntity originalEntity = new FollowJpaEntity(
                    followId,
                    followeeId,
                    followerId,
                    createdAt
            );

            Follow follow = mapper.toDomain(originalEntity);
            FollowJpaEntity entity = mapper.toJpaEntity(follow);

            assertThat(entity.getId()).isEqualTo(originalEntity.getId());
            assertThat(entity.getFollowerId()).isEqualTo(originalEntity.getFollowerId());
            assertThat(entity.getFolloweeId()).isEqualTo(originalEntity.getFolloweeId());
            assertThat(entity.getCreatedAt()).isEqualTo(originalEntity.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("다양한 UUID 값으로 매핑")
    class VariousUuidValues {

        @Test
        @DisplayName("성공: 다른 UUID 값들로 매핑한다")
        void different_uuid_values() {
            UUID id1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            UUID id2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
            UUID id3 = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");

            Follow follow = Follow.builder()
                    .id(id1)
                    .followerId(id2)
                    .followeeId(id3)
                    .createdAt(createdAt)
                    .build();

            FollowJpaEntity entity = mapper.toJpaEntity(follow);

            assertThat(entity.getId()).isEqualTo(id1);
            assertThat(entity.getFollowerId()).isEqualTo(id2);
            assertThat(entity.getFolloweeId()).isEqualTo(id3);
        }

        @Test
        @DisplayName("성공: 같은 매퍼 인스턴스로 여러 객체를 매핑한다")
        void multiple_mappings_with_same_mapper() {
            Follow follow1 = Follow.builder()
                    .id(UUID.randomUUID())
                    .followerId(UUID.randomUUID())
                    .followeeId(UUID.randomUUID())
                    .createdAt(Instant.now())
                    .build();

            Follow follow2 = Follow.builder()
                    .id(UUID.randomUUID())
                    .followerId(UUID.randomUUID())
                    .followeeId(UUID.randomUUID())
                    .createdAt(Instant.now())
                    .build();

            FollowJpaEntity entity1 = mapper.toJpaEntity(follow1);
            FollowJpaEntity entity2 = mapper.toJpaEntity(follow2);

            assertThat(entity1.getId()).isNotEqualTo(entity2.getId());
            assertThat(entity1.getFollowerId()).isNotEqualTo(entity2.getFollowerId());
        }
    }
}
