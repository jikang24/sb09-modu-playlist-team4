package com.mopl.global.outbox;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxStatusUpdater {

  private final OutboxRepository outboxRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markPublished(UUID eventId) {
    outboxRepository.findById(eventId).ifPresent(OutboxEvent::markPublished);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID eventId, int maxRetry) {
    outboxRepository.findById(eventId).ifPresent(event -> event.markFailed(maxRetry));
  }
}