package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

//JpaSpecificationExecutor: 동적 쿼리(검색/필터) 지원
interface ContentJpaRepository extends JpaRepository<ContentJpaEntity, UUID>,
    JpaSpecificationExecutor<ContentJpaEntity> {

  Optional<ContentJpaEntity> findByTypeAndExternalId(ContentType type, String externalId);

  /**
   * 타입별 externalId Set 조회
   * → Batch 수집 시 루프 안에서 매번 DB 조회하는 N+1 문제 방지
   * → 쿼리 1번으로 전체 externalId 가져온 후 Set.contains()로 중복 체크
   */
  @Query("SELECT c.externalId FROM ContentJpaEntity c WHERE c.type = :type")
  Set<String> findExternalIdsByType(@Param("type") ContentType type);
}