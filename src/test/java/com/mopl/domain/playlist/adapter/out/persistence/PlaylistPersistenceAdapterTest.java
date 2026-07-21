package com.mopl.domain.playlist.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import com.mopl.domain.playlist.application.dto.SortBy;
import com.mopl.domain.playlist.domain.Playlist;
import com.mopl.global.config.QueryDslConfig;
import com.mopl.global.dto.SortDirection;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import({PlaylistPersistenceAdapter.class, PlaylistPersistenceMapper.class, QueryDslConfig.class, PlaylistRepositoryCustomImpl.class})
class PlaylistPersistenceAdapterTest {

    @Autowired private PlaylistPersistenceAdapter adapter;
    @Autowired private PlaylistJpaRepository playlistJpaRepository;
    @Autowired private PlaylistSubscriptionJpaRepository subscriptionJpaRepository;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;

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

    @Test
    @DisplayName("findByIdForUpdate - 존재하면 조회, 없으면 빈 값")
    void findByIdForUpdate_success() {
        // given
        UUID ownerId = UUID.randomUUID();
        Playlist saved = adapter.save(Playlist.create(ownerId, "Title", "Desc"));

        // when & then
        assertThat(adapter.findByIdForUpdate(saved.getId())).isPresent();
        assertThat(adapter.findByIdForUpdate(UUID.randomUUID())).isEmpty();
    }

    /**
     * KAN-151: syncContents()가 "조회 시점 스냅샷 전체 diff" 방식이라, 잠금 없이 동시에
     * 서로 다른 콘텐츠를 추가하면 나중에 커밋되는 쪽이 먼저 추가된 콘텐츠를 모른 채 저장돼
     * 조용히 지워지는 lost update가 발생했다. findByIdForUpdate()의 SELECT ... FOR UPDATE로
     * 두 트랜잭션을 직렬화해서 막는지 검증 - 스레드 A가 행 잠금을 잡고 있는 동안 스레드 B의
     * findByIdForUpdate가 대기하다가, A가 커밋된 뒤에야 진행되어 A가 추가한 콘텐츠를 보고
     * 자기 콘텐츠를 더해야 한다. (테스트 트랜잭션과 분리된 커밋이 필요해 기본 테스트 트랜잭션을 끈다)
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동시에 서로 다른 콘텐츠를 추가해도 행 잠금으로 직렬화되어 lost update 없이 둘 다 저장된다")
    void findByIdForUpdate_preventsLostUpdateOnConcurrentContentAdd() throws Exception {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        UUID ownerId = UUID.randomUUID();
        UUID playlistId = txTemplate.execute(status ->
            adapter.save(Playlist.create(ownerId, "Title", "Desc")).getId());

        UUID contentA = UUID.randomUUID();
        UUID contentB = UUID.randomUUID();

        CountDownLatch aHoldingLock = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> futureA = executor.submit(() -> txTemplate.executeWithoutResult(status -> {
                Playlist playlist = adapter.findByIdForUpdate(playlistId).orElseThrow();
                aHoldingLock.countDown();
                awaitQuietly(releaseA);
                playlist.addContent(contentA);
                adapter.save(playlist);
            }));

            assertThat(aHoldingLock.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> futureB = executor.submit(() -> txTemplate.executeWithoutResult(status -> {
                // A가 행 잠금을 쥐고 있는 동안 여기서 블로킹돼야 한다
                Playlist playlist = adapter.findByIdForUpdate(playlistId).orElseThrow();
                playlist.addContent(contentB);
                adapter.save(playlist);
            }));

            // B가 잠금 대기 중인 상태를 잠깐 유지시킨 뒤 A를 커밋시켜 진행시킨다
            Thread.sleep(300);
            releaseA.countDown();

            futureA.get(5, TimeUnit.SECONDS);
            futureB.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Playlist result = txTemplate.execute(status -> adapter.findById(playlistId).orElseThrow());
            assertThat(result.getContentIds()).containsExactlyInAnyOrder(contentA, contentB);
        });
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
