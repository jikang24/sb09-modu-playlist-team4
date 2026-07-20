package com.mopl.domain.review.adapter.user;

import com.mopl.domain.review.adapter.port.LoadUserPort;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.UserSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// UserAdapter(대화방)와 똑같은 역할 - UserService를 호출해서 UserSummary로 변환만 함
// Review 모듈의 다른 코드(ReviewService 등)는 이 어댑터의 존재도 몰라도 됨
// (LoadUserPort 인터페이스만 보고 개발하면 되니까)
@Component
@RequiredArgsConstructor
public class ReviewUserAdapter implements LoadUserPort {

  private final UserService userService;

  @Override
  public UserSummary getUserSummary(UUID userId) {
    // Review는 이 값을 스냅샷으로 영구 저장하므로, 2시간 후 만료되는 presigned URL이 아니라
    // 원본 값(S3 key/외부 URL)을 받아야 한다. presigned URL 변환은 조회 시점에 따로 한다.
    var userDto = userService.findRaw(userId);
    return new UserSummary(userDto.id(), userDto.name(), userDto.profileImageUrl());
  }
}