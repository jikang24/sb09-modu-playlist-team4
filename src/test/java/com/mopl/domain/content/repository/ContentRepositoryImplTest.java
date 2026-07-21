package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.infrastructure.ContentMapper;
import com.mopl.global.config.JpaConfig;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @DataJpaTest 에는 @Transactional 이 포함되어 있어
 * @BeforeEach 와 각 테스트 메서드가 동일한 트랜잭션 안에서 실행됩니다.
 * @Nested 를 사용하면 @Transactional 이 중첩 클래스에 상속되지 않아
 * LazyInitializationException 이 발생하므로 메서드를 외부 클래스에 직접 선언합니다.
 */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:contenttest;MODE=PostgreSQL",
    "spring.sql.init.mode=never"
})
@Import({ContentRepositoryImpl.class, JpaConfig.class, ContentRepositoryImplTest.Config.class})
@DisplayName("ContentRepositoryImpl 통합 테스트")
class ContentRepositoryImplTest {

    @TestConfiguration
    static class Config {
        @Bean
        public ContentMapper contentMapper() {
            return new ContentMapper() {};
        }

        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }
    }

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentJpaRepository contentJpaRepository;

    @Autowired
    private EntityManager em;

    private Content movie1;
    private Content movie2;
    private Content sport1;
    private Instant nextCreatedAt = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        contentJpaRepository.deleteAll();
        movie1 = saveWithCreatedAt(ContentType.MOVIE, "tmdb-001", "어벤져스", List.of("액션", "SF"));
        movie2 = saveWithCreatedAt(ContentType.MOVIE, "tmdb-002", "인터스텔라", List.of("SF", "드라마"));
        sport1 = saveWithCreatedAt(ContentType.SPORT, "sports-001", "EPL 하이라이트", List.of("스포츠"));
        em.flush();
        em.clear();
    }

    private Content saveWithCreatedAt(ContentType type, String externalId, String title, List<String> tags) {
        Content content = Content.create(type, externalId, title, "설명", null, tags);
        Instant createdAt = nextCreatedAt;
        nextCreatedAt = nextCreatedAt.plusSeconds(1);
        return saveContentWithCreatedAt(content, createdAt);
    }

    private Content saveContentWithCreatedAt(Content content, Instant createdAt) {
        Content saved = contentRepository.save(content);
        em.createQuery(
                        "UPDATE ContentJpaEntity c SET c.createdAt = :createdAt WHERE c.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        em.flush();
        em.clear();
        return contentRepository.findById(saved.getId()).orElseThrow();
    }

    private Content save(ContentType type, String externalId, String title, List<String> tags) {
        return contentRepository.save(
            Content.create(type, externalId, title, "설명", null, tags)
        );
    }

    private ContentSearchRequest request(
        ContentType type, String keyword, List<String> tags,
        String cursor, UUID idAfter, int limit,
        String sortBy, String sortDirection
    ) {
        return new ContentSearchRequest(type, keyword, tags, cursor, idAfter, limit, sortBy, sortDirection);
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[save] 기본 필드가 정상적으로 저장되고 반환된다")
    void save_success_basicFields() {
        Content saved = save(ContentType.TV_SERIES, "tmdb-100", "드라마 제목", List.of());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getType()).isEqualTo(ContentType.TV_SERIES);
        assertThat(saved.getExternalId()).isEqualTo("tmdb-100");
        assertThat(saved.getTitle()).isEqualTo("드라마 제목");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("[save] 태그가 함께 저장된다")
    void save_success_withTags() {
        Content saved = save(ContentType.MOVIE, "tmdb-200", "태그 테스트", List.of("액션", "모험"));

        assertThat(saved.getTags()).containsExactlyInAnyOrder("액션", "모험");
    }

    @Test
    @DisplayName("[save] 기존 태그를 일부 유지한 채 수정해도 유니크 제약 위반 없이 갱신된다")
    void save_update_keepingOverlappingTag_doesNotViolateUniqueConstraint() {
        Content saved = save(ContentType.MOVIE, "tmdb-201", "겹치는 태그 테스트", List.of("액션", "모험"));
        em.flush();
        em.clear();

        Content loaded = contentRepository.findById(saved.getId()).orElseThrow();
        // "액션"은 그대로 유지, "모험"은 제거, "신규"는 추가 → 겹치는 태그가 있는 갱신
        loaded.update(loaded.getType(), loaded.getTitle(), loaded.getDescription(),
            loaded.getThumbnailUrl(), List.of("액션", "신규"));

        Content updated = contentRepository.save(loaded);
        em.flush(); // 여기서 예외 없이 flush 되어야 함 (버그: content_tags 유니크 제약 위반으로 500)

        assertThat(updated.getTags()).containsExactlyInAnyOrder("액션", "신규");

        em.clear();
        Content reloaded = contentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTags()).containsExactlyInAnyOrder("액션", "신규");
    }

    @Test
    @DisplayName("[save] 태그 값이 완전히 동일하면 재삽입 없이 그대로 유지된다")
    void save_update_withIdenticalTags_isNoOp() {
        Content saved = save(ContentType.MOVIE, "tmdb-202", "동일 태그 테스트", List.of("액션", "모험"));
        em.flush();
        em.clear();

        Content loaded = contentRepository.findById(saved.getId()).orElseThrow();
        loaded.update(loaded.getType(), loaded.getTitle(), loaded.getDescription(),
            loaded.getThumbnailUrl(), List.of("액션", "모험"));

        Content updated = contentRepository.save(loaded);
        em.flush();

        assertThat(updated.getTags()).containsExactlyInAnyOrder("액션", "모험");
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[findById] 존재하는 ID - Optional에 도메인 반환")
    void findById_found() {
        Optional<Content> result = contentRepository.findById(movie1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(movie1.getId());
        assertThat(result.get().getTitle()).isEqualTo("어벤져스");
    }

    @Test
    @DisplayName("[findById] 존재하지 않는 ID - Optional.empty() 반환")
    void findById_notFound() {
        Optional<Content> result = contentRepository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // ── findByTypeAndExternalId ───────────────────────────────────────────────

    @Test
    @DisplayName("[findByTypeAndExternalId] 일치하는 타입+외부ID - 도메인 반환")
    void findByTypeAndExternalId_found() {
        Optional<Content> result =
            contentRepository.findByTypeAndExternalId(ContentType.MOVIE, "tmdb-001");

        assertThat(result).isPresent();
        assertThat(result.get().getExternalId()).isEqualTo("tmdb-001");
    }

    @Test
    @DisplayName("[findByTypeAndExternalId] 타입이 다르면 - 반환 안 됨")
    void findByTypeAndExternalId_notFound_differentType() {
        Optional<Content> result =
            contentRepository.findByTypeAndExternalId(ContentType.SPORT, "tmdb-001");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[findByTypeAndExternalId] externalId가 없으면 - 반환 안 됨")
    void findByTypeAndExternalId_notFound_unknownExternalId() {
        Optional<Content> result =
            contentRepository.findByTypeAndExternalId(ContentType.MOVIE, "unknown");

        assertThat(result).isEmpty();
    }

    // ── findAllByType ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("[findAllByType] 해당 타입의 콘텐츠만 도메인으로 반환된다")
    void findAllByType_returnsOnlyMatchingType() {
        List<Content> result = contentRepository.findAllByType(ContentType.MOVIE);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Content::getTitle)
            .containsExactlyInAnyOrder("어벤져스", "인터스텔라");
    }

    @Test
    @DisplayName("[findAllByType] 해당 타입이 없으면 빈 리스트 반환")
    void findAllByType_emptyWhenNoMatch() {
        List<Content> result = contentRepository.findAllByType(ContentType.TV_SERIES);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[findAllByType] tags를 벌크 조회해 N+1 없이 쿼리 2번(content 1 + tags 1)만 발생한다")
    void findAllByType_doesNotCauseNPlusOneForTags() {
        org.hibernate.stat.Statistics statistics = em.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Content> result = contentRepository.findAllByType(ContentType.MOVIE);

        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(c -> assertThat(c.getTags()).isNotEmpty());
    }

    // ── findAllByIds ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("[findAllByIds] 주어진 ID들에 해당하는 콘텐츠만 반환된다")
    void findAllByIds_returnsMatchingContents() {
        List<Content> result = contentRepository.findAllByIds(List.of(movie1.getId(), sport1.getId()));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Content::getId)
            .containsExactlyInAnyOrder(movie1.getId(), sport1.getId());
    }

    @Test
    @DisplayName("[findAllByIds] 빈 리스트면 조회 없이 빈 리스트 반환")
    void findAllByIds_emptyList_returnsEmpty() {
        assertThat(contentRepository.findAllByIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("[findAllByIds] null이면 조회 없이 빈 리스트 반환")
    void findAllByIds_null_returnsEmpty() {
        assertThat(contentRepository.findAllByIds(null)).isEmpty();
    }

    @Test
    @DisplayName("[findAllByIds] tags를 벌크 조회해 N+1 없이 쿼리 2번(content 1 + tags 1)만 발생한다")
    void findAllByIds_doesNotCauseNPlusOneForTags() {
        org.hibernate.stat.Statistics statistics = em.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Content> result = contentRepository.findAllByIds(
                List.of(movie1.getId(), movie2.getId(), sport1.getId()));

        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(c -> assertThat(c.getTags()).isNotEmpty());
    }

    // ── findExternalIdsByType ─────────────────────────────────────────────────

    @Test
    @DisplayName("[findExternalIdsByType] 해당 타입의 externalId만 반환된다")
    void findExternalIdsByType_returnsOnlyMatchingType() {
        Set<String> movieIds = contentRepository.findExternalIdsByType(ContentType.MOVIE);

        assertThat(movieIds).containsExactlyInAnyOrder("tmdb-001", "tmdb-002");
    }

    @Test
    @DisplayName("[findExternalIdsByType] 해당 타입이 없으면 빈 Set 반환")
    void findExternalIdsByType_emptyWhenNoMatch() {
        Set<String> tvIds = contentRepository.findExternalIdsByType(ContentType.TV_SERIES);

        assertThat(tvIds).isEmpty();
    }

    // ── existsById / deleteById ───────────────────────────────────────────────

    @Test
    @DisplayName("[existsById] 저장된 ID - true")
    void existsById_true() {
        assertThat(contentRepository.existsById(movie1.getId())).isTrue();
    }

    @Test
    @DisplayName("[existsById] 없는 ID - false")
    void existsById_false() {
        assertThat(contentRepository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("[deleteById] 삭제 후 findById empty")
    void deleteById_removed() {
        contentRepository.deleteById(movie1.getId());
        em.flush();
        em.clear();

        assertThat(contentRepository.findById(movie1.getId())).isEmpty();
    }

    // ── findAllByCondition ────────────────────────────────────────────────────

    @Test
    @DisplayName("[findAllByCondition] 필터 없음 - 전체 3건 반환")
    void findAll_noFilter_returnsAll() {
        ContentSearchRequest req = request(null, null, null, null, null, 10, "createdAt", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("[findAllByCondition] tags를 벌크 조회해 N+1 없이 쿼리 2번(content 1 + tags 1)만 발생한다")
    void findAll_doesNotCauseNPlusOneForTags() {
        org.hibernate.stat.Statistics statistics = em.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        ContentSearchRequest req = request(null, null, null, null, null, 10, "createdAt", "DESCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        // content 목록 조회 1번 + tags 벌크 조회 1번 = 총 2번 (content 건수만큼 늘어나면 N+1)
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(c -> assertThat(c.getTags()).isNotEmpty());
    }

    @Test
    @DisplayName("[findAllByCondition] typeEqual=MOVIE - MOVIE 타입만 반환")
    void findAll_filterByType() {
        ContentSearchRequest req = request(ContentType.MOVIE, null, null, null, null, 10, "createdAt", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.getType() == ContentType.MOVIE);
    }

    @Test
    @DisplayName("[findAllByCondition] keywordLike - 제목 부분 검색")
    void findAll_filterByKeyword() {
        ContentSearchRequest req = request(null, "인터", null, null, null, 10, "createdAt", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("인터스텔라");
    }

    @Test
    @DisplayName("[findAllByCondition] tagsIn - 해당 태그 포함된 콘텐츠만 반환")
    void findAll_filterByTag() {
        ContentSearchRequest req = request(null, null, List.of("SF"), null, null, 10, "createdAt", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Content::getTitle)
            .containsExactlyInAnyOrder("어벤져스", "인터스텔라");
    }

    @Test
    @DisplayName("[findAllByCondition] tagsIn - 존재하지 않는 태그는 빈 결과")
    void findAll_filterByTag_noMatch() {
        ContentSearchRequest req = request(null, null, List.of("없는태그"), null, null, 10, "createdAt", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[findAllByCondition] DESCENDING 커서 - 커서 이전 항목만 반환")
    void findAll_cursorPagination_descending() {
        String cursor = sport1.getCreatedAt().toString();
        UUID idAfter = sport1.getId();

        ContentSearchRequest req = request(null, null, null, cursor, idAfter, 10, "createdAt", "DESCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).isNotEmpty();
        assertThat(result).noneMatch(c -> c.getId().equals(sport1.getId()));
    }

    @Test
    @DisplayName("[findAllByCondition] ASCENDING 커서 - 커서 이후 항목만 반환")
    void findAll_cursorPagination_ascending() {
        String cursor = movie1.getCreatedAt().toString();
        UUID idAfter = movie1.getId();

        ContentSearchRequest req = request(null, null, null, cursor, idAfter, 10, "createdAt", "ASCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).isNotEmpty();
        assertThat(result).noneMatch(c -> c.getId().equals(movie1.getId()));
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=rate(평점순) - averageRating 내림차순으로 정렬된다")
    void findAll_sortByRate_ordersByAverageRating() {
        // 프론트 "평점순" 옵션이 실제로 보내는 값은 sortBy=rate (averageRating이 아님)
        Content lowRated = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "rate-low", "낮은 평점", "설명", null,
            new BigDecimal("1.00"), 0, Instant.now(), Instant.now(), List.of()));
        Content highRated = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "rate-high", "높은 평점", "설명", null,
            new BigDecimal("4.50"), 0, Instant.now(), Instant.now(), List.of()));

        ContentSearchRequest req = request(null, null, null, null, null, 10, "rate", "DESCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        List<Content> ratedOnly = result.stream()
            .filter(c -> c.getId().equals(lowRated.getId()) || c.getId().equals(highRated.getId()))
            .toList();
        assertThat(ratedOnly).extracting(Content::getId)
            .containsExactly(highRated.getId(), lowRated.getId());
    }

    @Test
    @DisplayName("[findAllByCondition] 잘못된 커서 형식 - INVALID_CURSOR_FORMAT 예외")
    void findAll_invalidCursorFormat_throwsException() {
        ContentSearchRequest req = request(null, null, null, "잘못된커서", UUID.randomUUID(), 10, "createdAt", "DESCENDING");

        assertThatThrownBy(() -> contentRepository.findAllByCondition(req))
            .isInstanceOf(MoplException.class)
            .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT));
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=reviewCount(인기순) - 커서 이후(reviewCount 작은) 항목만 반환")
    void findAll_cursorPagination_reviewCount_descending() {
        Content lowCount = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "review-low", "적은 리뷰", "설명", null,
            BigDecimal.ZERO, 1, Instant.now(), Instant.now(), List.of()));
        Content highCount = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "review-high", "많은 리뷰", "설명", null,
            BigDecimal.ZERO, 100, Instant.now(), Instant.now(), List.of()));

        ContentSearchRequest req = request(null, null, null,
            String.valueOf(highCount.getReviewCount()), highCount.getId(), 10, "reviewCount", "DESCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).extracting(Content::getId).contains(lowCount.getId());
        assertThat(result).extracting(Content::getId).doesNotContain(highCount.getId());
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=reviewCount, ASCENDING - 커서 이후(reviewCount 큰) 항목만 반환")
    void findAll_cursorPagination_reviewCount_ascending() {
        Content lowCount = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "review-low-asc", "적은 리뷰", "설명", null,
            BigDecimal.ZERO, 1, Instant.now(), Instant.now(), List.of()));
        Content highCount = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "review-high-asc", "많은 리뷰", "설명", null,
            BigDecimal.ZERO, 100, Instant.now(), Instant.now(), List.of()));

        ContentSearchRequest req = request(null, null, null,
            String.valueOf(lowCount.getReviewCount()), lowCount.getId(), 10, "reviewCount", "ASCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).extracting(Content::getId).contains(highCount.getId());
        assertThat(result).extracting(Content::getId).doesNotContain(lowCount.getId());
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=watcherCount(인기순 별칭) - reviewCount 기준으로 취급된다")
    void findAll_sortByWatcherCount_treatedAsReviewCount() {
        ContentSearchRequest req = request(null, null, null, null, null, 10, "watcherCount", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=reviewCount, 잘못된 커서 형식 - INVALID_CURSOR_FORMAT 예외")
    void findAll_reviewCountCursor_invalidFormat_throwsException() {
        ContentSearchRequest req = request(null, null, null, "숫자아님", UUID.randomUUID(), 10, "reviewCount", "DESCENDING");

        assertThatThrownBy(() -> contentRepository.findAllByCondition(req))
            .isInstanceOf(MoplException.class)
            .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT));
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=rate(평점순) - 커서 이후(평점 낮은) 항목만 반환")
    void findAll_cursorPagination_rate_descending() {
        Content lowRated = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "rate-low2", "낮은 평점", "설명", null,
            new BigDecimal("1.00"), 0, Instant.now(), Instant.now(), List.of()));
        Content highRated = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "rate-high2", "높은 평점", "설명", null,
            new BigDecimal("4.50"), 0, Instant.now(), Instant.now(), List.of()));

        ContentSearchRequest req = request(null, null, null,
            highRated.getAverageRating().toString(), highRated.getId(), 10, "rate", "DESCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).extracting(Content::getId).contains(lowRated.getId());
        assertThat(result).extracting(Content::getId).doesNotContain(highRated.getId());
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=rate, ASCENDING - 커서 이후(평점 높은) 항목만 반환")
    void findAll_cursorPagination_rate_ascending() {
        Content lowRated = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "rate-low-asc", "낮은 평점", "설명", null,
            new BigDecimal("1.00"), 0, Instant.now(), Instant.now(), List.of()));
        Content highRated = contentRepository.save(Content.restore(
            UUID.randomUUID(), ContentType.MOVIE, "rate-high-asc", "높은 평점", "설명", null,
            new BigDecimal("4.50"), 0, Instant.now(), Instant.now(), List.of()));

        ContentSearchRequest req = request(null, null, null,
            lowRated.getAverageRating().toString(), lowRated.getId(), 10, "rate", "ASCENDING");
        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).extracting(Content::getId).contains(highRated.getId());
        assertThat(result).extracting(Content::getId).doesNotContain(lowRated.getId());
    }

    @Test
    @DisplayName("[findAllByCondition] sortBy=rate, 잘못된 커서 형식 - INVALID_CURSOR_FORMAT 예외")
    void findAll_rateCursor_invalidFormat_throwsException() {
        ContentSearchRequest req = request(null, null, null, "숫자아님", UUID.randomUUID(), 10, "rate", "DESCENDING");

        assertThatThrownBy(() -> contentRepository.findAllByCondition(req))
            .isInstanceOf(MoplException.class)
            .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT));
    }

    @Test
    @DisplayName("[findAllByCondition] limit 적용 - limit+1개까지만 조회")
    void findAll_limitApplied() {
        ContentSearchRequest req = request(null, null, null, null, null, 1, "createdAt", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("[findAllByCondition] 타입 + 키워드 복합 필터")
    void findAll_combinedFilter() {
        ContentSearchRequest req = request(ContentType.MOVIE, "어벤", null, null, null, 10, "createdAt", "DESCENDING");

        List<Content> result = contentRepository.findAllByCondition(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("어벤져스");
    }

    // ── countByCondition ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[countByCondition] 필터 없음 - 전체 3건")
    void count_noFilter() {
        ContentSearchRequest req = request(null, null, null, null, null, 10, "createdAt", "DESCENDING");

        assertThat(contentRepository.countByCondition(req)).isEqualTo(3);
    }

    @Test
    @DisplayName("[countByCondition] typeEqual=SPORT - 1건")
    void count_filterByType() {
        ContentSearchRequest req = request(ContentType.SPORT, null, null, null, null, 10, "createdAt", "DESCENDING");

        assertThat(contentRepository.countByCondition(req)).isEqualTo(1);
    }

    @Test
    @DisplayName("[countByCondition] 매칭 없는 조건 - 0건")
    void count_noMatch() {
        ContentSearchRequest req = request(ContentType.TV_SERIES, null, null, null, null, 10, "createdAt", "DESCENDING");

        assertThat(contentRepository.countByCondition(req)).isZero();
    }

    @Test
    @DisplayName("[applyRatingDelta] 리뷰 없던 콘텐츠에 첫 리뷰가 반영된다")
    void applyRatingDelta_firstReview() {
        contentRepository.applyRatingDelta(movie1.getId(), new BigDecimal("4.5"), 1);
        em.flush();
        em.clear();

        Content updated = contentRepository.findById(movie1.getId()).orElseThrow();
        assertThat(updated.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(updated.getReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("[applyRatingDelta] 순차 적용된 delta들이 누적되어 정확한 평균을 계산한다 (동시성 안전성의 핵심)")
    void applyRatingDelta_accumulatesCorrectly() {
        // 두 번의 원자적 UPDATE가 서로의 결과를 덮어쓰지 않고 누적되는지 검증
        // (기존 방식은 절대값을 재계산해 덮어써서 lost-update가 발생했음)
        contentRepository.applyRatingDelta(movie1.getId(), new BigDecimal("4.0"), 1);
        contentRepository.applyRatingDelta(movie1.getId(), new BigDecimal("2.0"), 1);
        em.flush();
        em.clear();

        Content updated = contentRepository.findById(movie1.getId()).orElseThrow();
        assertThat(updated.getAverageRating()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(updated.getReviewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("[applyRatingDelta] 마지막 리뷰가 삭제되면 평균이 0으로 초기화된다 (0으로 나누기 방지)")
    void applyRatingDelta_resetsToZeroWhenLastReviewRemoved() {
        contentRepository.applyRatingDelta(movie1.getId(), new BigDecimal("4.0"), 1);
        contentRepository.applyRatingDelta(movie1.getId(), new BigDecimal("-4.0"), -1);
        em.flush();
        em.clear();

        Content updated = contentRepository.findById(movie1.getId()).orElseThrow();
        assertThat(updated.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updated.getReviewCount()).isZero();
    }
}
