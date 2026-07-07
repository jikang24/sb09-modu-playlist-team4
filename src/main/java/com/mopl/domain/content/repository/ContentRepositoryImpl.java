package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentSortField;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import com.mopl.domain.content.infrastructure.ContentMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * [Adapter Out] ContentRepository 구현체
 *
 * 순수 도메인 ↔ JPA 엔티티 변환을 ContentMapper가 담당
 * Service는 Content(순수 도메인)만 다루고
 * JPA 기술은 이 클래스 안에서만 사용됨
 */
@Repository
@RequiredArgsConstructor
public class ContentRepositoryImpl implements ContentRepository {

  private final ContentJpaRepository jpaRepository;
  private final ContentMapper contentMapper;  // MapStruct 생성 구현체 주입

  @Override
  public Content save(Content domain) {
    // 기존에 저장된 콘텐츠면 관리 중인 엔티티를 그대로 갱신 (신규 엔티티로 통째로
    // 갈아끼우면 태그 컬렉션이 매번 새 객체가 되어 merge 시 삭제보다 삽입이 먼저 실행되고,
    // 겹치는 태그가 있을 때 content_tags(content_id, tag) 유니크 제약 위반이 남)
    Optional<ContentJpaEntity> existing = jpaRepository.findById(domain.getId());

    ContentJpaEntity entity;
    if (existing.isPresent()) {
      entity = existing.get();
      entity.update(domain.getTitle(), domain.getDescription(), domain.getThumbnailUrl(),
          domain.getAverageRating(), domain.getReviewCount());
      entity.syncTags(domain.getTags());
    } else {
      // 신규 콘텐츠 → 도메인 → JPA 엔티티 변환 후 저장
      entity = contentMapper.toJpaEntity(domain);
    }

    ContentJpaEntity saved = jpaRepository.save(entity);
    // 저장된 JPA 엔티티 → 도메인으로 다시 변환해서 반환
    return contentMapper.toDomain(saved);
  }

  @Override
  public Optional<Content> findById(UUID id) {
    return jpaRepository.findById(id)
        .map(contentMapper::toDomain);
  }

  @Override
  public Optional<Content> findByTypeAndExternalId(ContentType type, String externalId) {
    return jpaRepository.findByTypeAndExternalId(type, externalId)
        .map(contentMapper::toDomain);
  }

  @Override
  public Set<String> findExternalIdsByType(ContentType type) {
    return jpaRepository.findExternalIdsByType(type);
  }

  @Override
  public List<Content> findAllByType(ContentType type) {
    return contentMapper.toDomainList(jpaRepository.findByType(type));
  }

  @Override
  public List<Content> findAllByCondition(ContentSearchRequest request) {
    // 정렬 방향 결정
    Sort.Direction direction = "ASCENDING".equalsIgnoreCase(request.sortDirection())
        ? Sort.Direction.ASC : Sort.Direction.DESC;

    // sortBy 필드 (createdAt 기본값, 프론트의 "인기순"=watcherCount는 reviewCount로 매핑)
    ContentSortField sortField = ContentSortField.resolve(request.sortBy());

    // limit+1개 조회 → hasNext 판단용
    PageRequest pageRequest = PageRequest.of(0, request.limit() + 1,
        Sort.by(direction, sortField.propertyName()).and(Sort.by(direction, "id")));

    Specification<ContentJpaEntity> spec = ContentSpecification.byCondition(request, sortField);

    return jpaRepository.findAll(spec, pageRequest)
        .stream()
        .map(contentMapper::toDomain)
        .toList();
  }

  @Override
  public long countByCondition(ContentSearchRequest request) {
    // 커서는 빼고 필터 조건만으로 카운트 (페이지가 넘어가도 totalCount는 동일해야 함)
    return jpaRepository.count(ContentSpecification.byFilter(request));
  }

  @Override
  public void deleteById(UUID id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public boolean existsById(UUID id) {
    return jpaRepository.existsById(id);
  }

  @Override
  public List<Content> findAllByIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return jpaRepository.findAllById(ids).stream()
        .map(contentMapper::toDomain)
        .toList();
  }
}