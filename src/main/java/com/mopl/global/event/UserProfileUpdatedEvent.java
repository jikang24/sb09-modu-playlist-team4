package com.mopl.global.event;

import java.util.UUID;

/**
 * [Event] 사용자 프로필(이름/프로필사진) 변경 이벤트
 *
 * global/event에 위치해서 User 모듈과 다른 모듈(Review 등) 양쪽에서 참조 가능
 * (User 모듈이 다른 모듈의 존재를 몰라도 되게 하기 위함 - ReviewRatingUpdatedEvent와 동일한 이유)
 *
 * 발행 (User 모듈, UserServiceImpl.updateProfile()):
 *   eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId, name, imageUrl));
 *
 * 구독 (Review 모듈, ReviewAuthorSyncListener):
 *   이 유저가 쓴 모든 리뷰의 author 스냅샷(authorName, authorProfileImageUrl)을 갱신
 */
public record UserProfileUpdatedEvent(
    UUID userId,
    String newName,
    String newProfileImageUrl
) {}
