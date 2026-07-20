package com.mopl.global.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * [Event] 리뷰 평점 변경 이벤트 - 절대값이 아니라 변화량(delta)을 담는다
 *
 * 모듈간 직접 의존 없이 통신하기 위한 이벤트
 *  * 발행 (Review 모듈):
 *  *   eventPublisher.publishEvent(new ReviewRatingUpdatedEvent(contentId, ratingDelta, countDelta));
 *
 * Content 쪽에서 절대값(avg/count)을 재계산해 덮어쓰면, 동시에 여러 리뷰가
 * 생성/삭제될 때 lost-update가 발생한다. 대신 DB에서
 * "review_count = review_count + countDelta" 식으로 원자적 UPDATE를 적용해서
 * 동시 요청에도 정확한 값이 나오게 한다.
 *
 * global/event에 위치해서 Review 모듈과 Content 모듈 양쪽에서 참조 가능
 */
public record ReviewRatingUpdatedEvent(
    UUID contentId,
    BigDecimal ratingDelta,
    int countDelta
) {}
