package com.mopl.domain.content.event;

import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRatingUpdatedEventListener {

  private final ContentRepository contentRepository;
  private final SearchContentPort searchContentPort;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleReviewRatingUpdated(ReviewRatingUpdatedEvent event) {

    Content content = contentRepository.findById(event.contentId())
        .orElseThrow(() -> new MoplException(ErrorCode.CONTENT_NOT_FOUND));

    content.updateRatingStats(
        event.averageRating(),
        event.reviewCount()
    );

    Content saved = contentRepository.save(content);

    try {
      searchContentPort.save(saved);
    } catch (Exception e) {
      log.error("[Content] OpenSearch 평점 갱신 실패 - id: {}", event.contentId(), e);
    }

    log.info("[Content] 평점 갱신 완료 - id: {}", event.contentId());
  }
}