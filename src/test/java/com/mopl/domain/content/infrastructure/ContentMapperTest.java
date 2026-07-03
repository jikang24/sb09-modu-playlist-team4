package com.mopl.domain.content.infrastructure;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContentMapper 테스트")
class ContentMapperTest {

    private final ContentMapper mapper = new ContentMapper() {};

    private Content makeDomain(ContentType type, String externalId, List<String> tags) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        return Content.restore(
            id, type, externalId,
            "제목", "설명", "https://thumb.jpg",
            new BigDecimal("3.50"), 5,
            now, now, tags
        );
    }

    private ContentJpaEntity makeJpaEntity(ContentType type, String externalId, List<String> tags) {
        ContentJpaEntity entity = ContentJpaEntity.of(
            UUID.randomUUID(), type, externalId,
            "JPA 제목", "JPA 설명", "https://jpa.jpg",
            new BigDecimal("4.00"), 10
        );
        List<ContentTagJpaEntity> tagEntities = tags.stream()
            .map(tag -> ContentTagJpaEntity.of(entity, tag))
            .toList();
        entity.replaceTags(tagEntities);
        return entity;
    }

    @Test
    @DisplayName("toJpaEntity - 도메인 기본 필드가 JPA 엔티티로 변환된다")
    void toJpaEntity_mapsBasicFields() {
        Content domain = makeDomain(ContentType.MOVIE, "tmdb-001", List.of());

        ContentJpaEntity entity = mapper.toJpaEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getType()).isEqualTo(ContentType.MOVIE);
        assertThat(entity.getExternalId()).isEqualTo("tmdb-001");
        assertThat(entity.getTitle()).isEqualTo("제목");
        assertThat(entity.getDescription()).isEqualTo("설명");
        assertThat(entity.getThumbnailUrl()).isEqualTo("https://thumb.jpg");
        assertThat(entity.getAverageRating()).isEqualByComparingTo(new BigDecimal("3.50"));
        assertThat(entity.getReviewCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("toJpaEntity - 태그가 ContentTagJpaEntity 리스트로 변환된다")
    void toJpaEntity_mapsTags() {
        Content domain = makeDomain(ContentType.TV_SERIES, "tmdb-002", List.of("드라마", "로맨스"));

        ContentJpaEntity entity = mapper.toJpaEntity(domain);

        assertThat(entity.getTags()).hasSize(2);
        assertThat(entity.getTags())
            .extracting(ContentTagJpaEntity::getTag)
            .containsExactly("드라마", "로맨스");
    }

    @Test
    @DisplayName("toJpaEntity - 태그 없을 때 빈 리스트")
    void toJpaEntity_emptyTags() {
        Content domain = makeDomain(ContentType.SPORT, "sports-001", List.of());

        ContentJpaEntity entity = mapper.toJpaEntity(domain);

        assertThat(entity.getTags()).isEmpty();
    }

    @Test
    @DisplayName("toDomain - JPA 엔티티 기본 필드가 도메인으로 변환된다")
    void toDomain_mapsBasicFields() {
        ContentJpaEntity entity = makeJpaEntity(ContentType.MOVIE, "tmdb-003", List.of());

        Content domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getType()).isEqualTo(ContentType.MOVIE);
        assertThat(domain.getExternalId()).isEqualTo("tmdb-003");
        assertThat(domain.getTitle()).isEqualTo("JPA 제목");
        assertThat(domain.getDescription()).isEqualTo("JPA 설명");
        assertThat(domain.getThumbnailUrl()).isEqualTo("https://jpa.jpg");
        assertThat(domain.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(domain.getReviewCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("toDomain - 태그가 문자열 리스트로 변환된다")
    void toDomain_mapsTags() {
        ContentJpaEntity entity = makeJpaEntity(ContentType.TV_SERIES, "tmdb-004", List.of("액션", "SF"));

        Content domain = mapper.toDomain(entity);

        assertThat(domain.getTags()).containsExactly("액션", "SF");
    }

    @Test
    @DisplayName("toDomainList - 엔티티 리스트가 도메인 리스트로 변환된다")
    void toDomainList_mapsAll() {
        List<ContentJpaEntity> entities = List.of(
            makeJpaEntity(ContentType.MOVIE, "tmdb-005", List.of("액션")),
            makeJpaEntity(ContentType.SPORT, "sports-002", List.of())
        );

        List<Content> domains = mapper.toDomainList(entities);

        assertThat(domains).hasSize(2);
        assertThat(domains.get(0).getType()).isEqualTo(ContentType.MOVIE);
        assertThat(domains.get(1).getType()).isEqualTo(ContentType.SPORT);
    }

    @Test
    @DisplayName("toDomainList - 빈 리스트 입력 시 빈 리스트 반환")
    void toDomainList_emptyList() {
        List<Content> domains = mapper.toDomainList(List.of());

        assertThat(domains).isEmpty();
    }
}
