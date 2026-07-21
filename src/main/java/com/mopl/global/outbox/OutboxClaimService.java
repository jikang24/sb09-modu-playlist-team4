package com.mopl.global.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PENDING 이벤트를 원자적으로 선점(claim)하는 역할만 담당.
 * 클레임(DB 트랜잭션)과 실제 카프카 발행(비동기 I/O)의 트랜잭션 경계를 분리하기 위해 별도 서비스로 뺐다.
 */
@Service
@RequiredArgsConstructor
public class OutboxClaimService {

  private final OutboxRepository outboxRepository;

  @Transactional
  public List<OutboxEvent> claimBatch(int limit) {
    List<java.util.UUID> ids = outboxRepository.findPendingIdsForUpdateSkipLocked(limit);
    if (ids.isEmpty()) {
      return List.of();
    }
    outboxRepository.markProcessing(ids);
    return outboxRepository.findAllById(ids);
  }
}