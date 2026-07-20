package com.mopl.domain.playlist.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import com.mopl.domain.playlist.application.dto.SortBy;
import com.mopl.domain.playlist.domain.Playlist;
import com.mopl.global.config.QueryDslConfig;
import com.mopl.global.dto.SortDirection;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import({PlaylistPersistenceAdapter.class, PlaylistPersistenceMapper.class, QueryDslConfig.class, PlaylistRepositoryCustomImpl.class})
class PlaylistPersistenceAdapterTest {

    @Autowired private PlaylistPersistenceAdapter adapter;
    @Autowired private PlaylistJpaRepository playlistJpaRepository;
    @Autowired private PlaylistSubscriptionJpaRepository subscriptionJpaRepository;
    @Autowired private EntityManager em;

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

    @Test
    @DisplayName("목록 조회 시 contents를 벌크 조회해 N+1 없이 쿼리 2번(playlist 1 + contents 1)만 발생한다")
    void findAllByCondition_doesNotCauseNPlusOneForContents() {
        // given: content를 가진 playlist 3개 저장
        // (신규 생성 저장 시점엔 syncContents가 안 타므로, 저장 → 조회 → content 추가 → 재저장 순으로 구성)
        UUID ownerId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            Playlist created = adapter.save(Playlist.create(ownerId, "Title" + i, "Desc" + i));
            Playlist playlist = adapter.findById(created.getId()).orElseThrow();
            playlist.addContent(UUID.randomUUID());
            playlist.addContent(UUID.randomUUID());
            adapter.save(playlist);
        }
        em.flush();
        em.clear();

        Statistics statistics = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // when
        PlaylistSearchCondition condition = new PlaylistSearchCondition(
                null, ownerId, null, null, null, 10, SortBy.updatedAt, SortDirection.DESCENDING);
        List<Playlist> result = adapter.findAllByCondition(condition);

        // then: playlist 목록 조회 1번 + contents 벌크 조회 1번 = 총 2번 (playlist 건수만큼 늘어나면 N+1)
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(p -> assertThat(p.getContentIds()).hasSize(2));
    }
}
