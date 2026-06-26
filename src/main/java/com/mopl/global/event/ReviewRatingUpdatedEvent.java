package com.mopl.global.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * [Event] 리뷰 평점 변경 이벤트
 *
 * 모듈간 직접 의존 없이 통신하기 위한 이벤트
 *  * 발행 (Review 모듈):
 *  *   eventPublisher.publishEvent(new ReviewRatingUpdatedEvent(contentId, avg, count));
 *
 * global/event에 위치해서 Review 모듈과 Content 모듈 양쪽에서 참조 가능
 */
public record ReviewRatingUpdatedEvent(
    UUID contentId,
    BigDecimal averageRating,
    int reviewCount
) {}
