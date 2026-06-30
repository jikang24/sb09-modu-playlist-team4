package com.mopl.domain.content.infrastructure;

import com.mopl.domain.content.domain.Content;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 순수 도메인 ↔ JPA 엔티티 변환 매퍼
 *
 * 변환 흐름:
 *   저장: Content(도메인) → ContentJpaEntity → DB
 *   조회: DB → ContentJpaEntity → Content(도메인)
 */
@Mapper(componentModel = "spring")
public interface ContentMapper {

  /**
   * 순수 도메인 → JPA 엔티티 변환
   * ContentRepositoryImpl.save() 에서 호출
   */
  default ContentJpaEntity toJpaEntity(Content domain) {
    ContentJpaEntity entity = ContentJpaEntity.of(
        domain.getId(),
        domain.getType(),
        domain.getExternalId(),
        domain.getTitle(),
        domain.getDescription(),
        domain.getThumbnailUrl(),
        domain.getAverageRating(),
        domain.getReviewCount()
    );

    // 태그 변환
    List<ContentTagJpaEntity> tagEntities = domain.getTags().stream()
        .map(tag -> ContentTagJpaEntity.of(entity, tag))
        .toList();
    entity.replaceTags(tagEntities);

    return entity;
  }

  /**
   * JPA 엔티티 → 순수 도메인 변환
   * ContentRepositoryImpl 조회 메서드에서 호출
   */
  default Content toDomain(ContentJpaEntity entity) {
    List<String> tags = entity.getTags().stream()
        .map(ContentTagJpaEntity::getTag)
        .toList();

    return Content.restore(
        entity.getId(),
        entity.getType(),
        entity.getExternalId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getThumbnailUrl(),
        entity.getAverageRating(),
        entity.getReviewCount(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        tags
    );
  }

  /** JPA 엔티티 리스트 → 도메인 리스트 변환 */
  default List<Content> toDomainList(List<ContentJpaEntity> entities) {
    return entities.stream().map(this::toDomain).toList();
  }
}