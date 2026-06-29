package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import com.mopl.domain.content.infrastructure.ContentMapper;
import lombok.RequiredArgsConstructor;
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
    // 도메인 → JPA 엔티티 변환 후 저장
    ContentJpaEntity entity = contentMapper.toJpaEntity(domain);
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
  public List<Content> findAll() {
    return contentMapper.toDomainList(jpaRepository.findAll());
  }

  @Override
  public List<Content> findAllByType(ContentType type) {
    return contentMapper.toDomainList(jpaRepository.findAllByType(type));
  }

  @Override
  public void deleteById(UUID id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public boolean existsById(UUID id) {
    return jpaRepository.existsById(id);
  }
}