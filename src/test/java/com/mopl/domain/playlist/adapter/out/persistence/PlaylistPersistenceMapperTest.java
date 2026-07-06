package com.mopl.domain.playlist.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.domain.playlist.domain.Playlist;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaylistPersistenceMapperTest {

    private PlaylistPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PlaylistPersistenceMapper();
    }

    @Test
    @DisplayName("JPA 엔티티를 도메인 객체로 변환 성공")
    void toDomain_success() {
        // given
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Instant now = Instant.now();
        
        PlaylistJpaEntity entity = PlaylistJpaEntity.of(id, ownerId, "Title", "Desc", now, now);
        entity.updateContents(List.of(PlaylistContentJpaEntity.of(contentId, 0)));

        // when
        Playlist domain = mapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getOwnerId()).isEqualTo(ownerId);
        assertThat(domain.getTitle()).isEqualTo("Title");
        assertThat(domain.getContentIds()).containsExactly(contentId);
    }

    @Test
    @DisplayName("도메인 객체를 새 JPA 엔티티로 변환 성공")
    void toJpaEntity_new_success() {
        // given
        UUID userId = UUID.randomUUID();
        Playlist domain = Playlist.create(userId, "Title", "Desc");
        UUID contentId = UUID.randomUUID();
        domain.addContent(contentId);

        // when
        PlaylistJpaEntity entity = mapper.toJpaEntity(domain, null);

        // then
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getOwnerId()).isEqualTo(domain.getOwnerId());
        assertThat(entity.getTitle()).isEqualTo(domain.getTitle());
        assertThat(entity.getContents()).hasSize(1);
        assertThat(entity.getContents().get(0).getContentId()).isEqualTo(contentId);
    }

    @Test
    @DisplayName("기존 JPA 엔티티 업데이트 변환 성공")
    void toJpaEntity_existing_success() {
        // given
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        PlaylistJpaEntity existing = PlaylistJpaEntity.of(id, ownerId, "Old Title", "Old Desc", now, now);
        
        Playlist domain = Playlist.restore(id, ownerId, "New Title", "New Desc", now, now, List.of());

        // when
        PlaylistJpaEntity updated = mapper.toJpaEntity(domain, existing);

        // then
        assertThat(updated).isSameAs(existing);
        assertThat(updated.getTitle()).isEqualTo("New Title");
    }
}
