package com.mopl.domain.follow.adapter.out.user;

import com.mopl.domain.follow.application.port.out.LoadUserPort;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.UserSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPort {

  private final UserService userService;

  @Override
  public UserSummary getUserSummary(UUID userId) {
    var userDto = userService.find(userId);
    return new UserSummary(userDto.id(), userDto.name(), userDto.profileImageUrl());
  }
}
