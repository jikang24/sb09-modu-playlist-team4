package com.mopl.domain.playlist.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.domain.playlist.domain.Playlist;
import com.mopl.global.config.QueryDslConfig;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({PlaylistPersistenceAdapter.class, PlaylistPersistenceMapper.class, QueryDslConfig.class, PlaylistRepositoryCustomImpl.class})
class PlaylistPersistenceAdapterTest {

    @Autowired private PlaylistPersistenceAdapter adapter;
    @Autowired private PlaylistJpaRepository playlistJpaRepository;
    @Autowired private PlaylistSubscriptionJpaRepository subscriptionJpaRepository;

    @Test
    @DisplayName("플레이리스트 저장 및 조회 성공")
    void saveAndFind_success() {
        // given
        UUID userId = UUID.randomUUID();
        Playlist playlist = Playlist.create(userId, "Title", "Desc");

        // when
        Playlist saved = adapter.save(playlist);
        Optional<Playlist> found = adapter.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Title");
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void delete_success() {
        // given
        UUID userId = UUID.randomUUID();
        Playlist saved = adapter.save(Playlist.create(userId, "Title", "Desc"));

        // when
        adapter.delete(saved.getId());

        // then
        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("구독 및 구독 확인 성공")
    void subscribe_success() {
        // given
        UUID ownerId = UUID.randomUUID();
        Playlist saved = adapter.save(Playlist.create(ownerId, "Title", "Desc"));
        UUID subscriberId = UUID.randomUUID();

        // when
        adapter.subscribe(saved.getId(), subscriberId);

        // then
        assertThat(adapter.isSubscribed(saved.getId(), subscriberId)).isTrue();
        assertThat(adapter.countSubscribers(saved.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("구독 취소 성공")
    void unsubscribe_success() {
        // given
        UUID ownerId = UUID.randomUUID();
        Playlist saved = adapter.save(Playlist.create(ownerId, "Title", "Desc"));
        UUID subscriberId = UUID.randomUUID();
        adapter.subscribe(saved.getId(), subscriberId);

        // when
        adapter.unsubscribe(saved.getId(), subscriberId);

        // then
        assertThat(adapter.isSubscribed(saved.getId(), subscriberId)).isFalse();
    }
}
