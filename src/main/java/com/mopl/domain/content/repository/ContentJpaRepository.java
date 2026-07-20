package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ContentJpaRepository extends JpaRepository<ContentJpaEntity, UUID> {

  Optional<ContentJpaEntity> findByTypeAndExternalId(ContentType type, String externalId);

  /**
   * 타입별 externalId Set 조회
   * → Batch 수집 시 루프 안에서 매번 DB 조회하는 N+1 문제 방지
   * → 쿼리 1번으로 전체 externalId 가져온 후 Set.contains()로 중복 체크
   */
  @Query("SELECT c.externalId FROM ContentJpaEntity c WHERE c.type = :type")
  Set<String> findExternalIdsByType(@Param("type") ContentType type);

  /**
   * 타입별 전체 엔티티 조회
   * → Batch 수집 시 신규/기존(썸네일 보정 대상) 판별에 사용 (쿼리 1번으로 N+1 방지)
   */
  List<ContentJpaEntity> findByType(ContentType type);

  /**
   * review_count/average_rating을 절대값으로 덮어쓰지 않고 원자적으로 증감시킨다.
   * 동시에 여러 리뷰가 생성/삭제돼도 lost-update 없이 정확한 값이 나오게 하기 위함
   * (read-modify-write를 애플리케이션이 아니라 DB가 한 문장으로 처리).
   */
  @Modifying
  @Query(value = """
      UPDATE contents
      SET review_count = review_count + :countDelta,
          average_rating = CASE
              WHEN (review_count + :countDelta) <= 0 THEN 0
              ELSE ROUND(((average_rating * review_count) + :ratingDelta) / (review_count + :countDelta), 1)
          END
      WHERE id = :contentId
      """, nativeQuery = true)
  void applyRatingDelta(
      @Param("contentId") UUID contentId,
      @Param("ratingDelta") BigDecimal ratingDelta,
      @Param("countDelta") int countDelta);
}