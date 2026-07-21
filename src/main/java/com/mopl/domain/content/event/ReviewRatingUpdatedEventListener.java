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
    // 절대값을 덮어쓰지 않고 DB에서 원자적으로 증감시켜, 동시에 여러 리뷰가
    // 생성/삭제돼도 lost-update 없이 정확한 값이 나오게 한다.
    contentRepository.applyRatingDelta(event.contentId(), event.ratingDelta(), event.countDelta());

    // applyRatingDelta가 캐시를 evict했으므로, 검색 인덱스 동기화를 위해 반영된 최신값을 다시 조회
    Content updated = contentRepository.findById(event.contentId())
        .orElseThrow(() -> new MoplException(ErrorCode.CONTENT_NOT_FOUND));

    try {
      searchContentPort.save(updated);
    } catch (Exception e) {
      log.error("[Content] OpenSearch 평점 갱신 실패 - id: {}", event.contentId(), e);
    }

    log.info("[Content] 평점 갱신 완료 - id: {}", event.contentId());
  }
}