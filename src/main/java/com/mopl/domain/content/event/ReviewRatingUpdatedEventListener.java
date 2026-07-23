package com.mopl.domain.content.event;

import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRatingUpdatedEventListener {

  private final ContentRepository contentRepository;
  private final SearchContentPort searchContentPort;

  /**
   * AFTER_COMMIT 시점엔 리뷰 쪽 원본 트랜잭션이 이미 끝났으므로, 참여할 트랜잭션이 없다.
   * @Transactional 없이 두면 applyRatingDelta(@Modifying 쿼리)가 활성 트랜잭션이 없다는
   * 이유로 예외를 던지는데, 이 시점의 예외는 afterCompletion 콜백 안에서 로그만 남고
   * 호출자에게 전파되지 않아 평점 갱신이 조용히 실패한다 - REQUIRES_NEW로 새 트랜잭션을 연다.
   * @Async: 별도 스레드로 넘겨서 리뷰 작성 응답이 이 처리를 기다리지 않게 한다.
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleReviewRatingUpdated(ReviewRatingUpdatedEvent event) {
    Content updated;
    try {
      // 절대값을 덮어쓰지 않고 DB에서 원자적으로 증감시켜, 동시에 여러 리뷰가
      // 생성/삭제돼도 lost-update 없이 정확한 값이 나오게 한다.
      contentRepository.applyRatingDelta(event.contentId(), event.ratingDelta(), event.countDelta());

      // applyRatingDelta가 캐시를 evict했으므로, 검색 인덱스 동기화를 위해 반영된 최신값을 다시 조회
      updated = contentRepository.findById(event.contentId())
          .orElseThrow(() -> new MoplException(ErrorCode.CONTENT_NOT_FOUND));
    } catch (Exception e) {
      // @Async라 여기서 못 던지면(afterCompletion 콜백에서 터진 예외는 호출자에게 전파 안 됨)
      // 평점 갱신 자체가 조용히 유실된다 - 무슨 이벤트가 실패했는지 바로 검색되도록 남겨둔다.
      log.error("[Content] 평점 갱신 실패 - event={}", event, e);
      return;
    }

    try {
      searchContentPort.save(updated);
    } catch (Exception e) {
      log.error("[Content] OpenSearch 평점 갱신 실패 - id: {}", event.contentId(), e);
    }

    log.info("[Content] 평점 갱신 완료 - id: {}", event.contentId());
  }
}