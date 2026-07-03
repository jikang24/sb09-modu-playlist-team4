package com.mopl.domain.watchingsession.adapter.user;

import com.mopl.domain.user.service.UserService;
import com.mopl.domain.watchingsession.adapter.port.LoadUserPort;
import com.mopl.global.dto.UserSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchingSessionUserAdapter implements LoadUserPort {

  private final UserService userService;

  @Override
  public UserSummary getUserSummary(UUID userId) {
    var userDto = userService.find(userId);
    return new UserSummary(userDto.id(), userDto.name(), userDto.profileImageUrl());
  }
}