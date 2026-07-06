package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;

import com.mopl.domain.content.dto.ContentSearchRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


public interface ContentRepository {

  Content save(Content content);

  Optional<Content> findById(UUID id);

  /** 타입 + 외부ID 조회 - 중복 수집 방지용 */
  Optional<Content> findByTypeAndExternalId(ContentType type, String externalId);

  /**
   * 타입별 externalId 목록 조회 (N+1 방지용)
   * Batch 수집 시 중복 확인을 위해 한번에 조회
   */
  Set<String> findExternalIdsByType(ContentType type);

  List<Content> findAllByCondition(ContentSearchRequest request);

  /** 검색 조건에 맞는 전체 개수 (totalCount용) */
  long countByCondition(ContentSearchRequest request);


  void deleteById(UUID id);

  boolean existsById(UUID id);

  List<Content> findAllByIds(List<UUID> ids);
}
