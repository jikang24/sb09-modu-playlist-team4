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
    var userDto = userService.find(userId);
    return new UserSummary(userDto.id(), userDto.name(), userDto.profileImageUrl());
  }
}