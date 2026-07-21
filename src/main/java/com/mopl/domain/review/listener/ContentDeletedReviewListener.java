package com.mopl.domain.review.listener;

import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.event.ContentDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 콘텐츠가 삭제됐을 때, 그 콘텐츠에 달린 리뷰가 고아로 남지 않도록 함께 삭제
 * @Async + AFTER_COMMIT: 콘텐츠 삭제 트랜잭션이 커밋된 후, 별도 스레드에서 처리
 */
@Component
@RequiredArgsConstructor
public class ContentDeletedReviewListener {

  private final ReviewRepository reviewRepository;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ContentDeletedEvent event) {
    reviewRepository.deleteByContentId(event.contentId());
  }
}