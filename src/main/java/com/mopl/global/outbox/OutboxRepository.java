package com.mopl.global.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

  /**
   * PENDING 상태인 이벤트의 id를 잠금과 함께 선점한다.
   * FOR UPDATE SKIP LOCKED: 다른 트랜잭션(다른 스케줄러 인스턴스)이 이미 잡고 있는 row는 건너뛰므로
   * 멀티 인스턴스 환경에서도 같은 row를 동시에 집어가지 않는다.
   * (PostgreSQL / MySQL 8.0+ 기준. 다른 DB라면 방언에 맞게 조정 필요)
   */
  @Query(value = """
      SELECT id FROM outbox_event
      WHERE status = 'PENDING'
      ORDER BY created_at ASC
      LIMIT :limit
      FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<UUID> findPendingIdsForUpdateSkipLocked(@Param("limit") int limit);

  @Modifying
  @Query("UPDATE OutboxEvent e SET e.status = 'PROCESSING', e.claimedAt = CURRENT_TIMESTAMP WHERE e.id IN :ids")
  void markProcessing(@Param("ids") List<UUID> ids);

  @Modifying
  @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :threshold")
  int deletePublishedBefore(@Param("threshold") LocalDateTime threshold);

  /**
   * 장애로 인해 PROCESSING 상태에서 멈춘 채 오랫동안 방치된 이벤트를 복구용으로 조회한다.
   * (relay 인스턴스가 send 도중 죽는 경우 대비)
   */
  @Modifying
  @Query("""
      UPDATE OutboxEvent e SET e.status = 'PENDING', e.claimedAt = NULL
      WHERE e.status = 'PROCESSING' AND e.claimedAt < :threshold
      """)
  int recoverStuckProcessing(@Param("threshold") LocalDateTime threshold);
}