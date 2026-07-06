package com.mopl.domain.watchingsession.adapter.user;

import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.service.UserService;
import com.mopl.domain.watchingsession.adapter.port.LoadUserPort;
import com.mopl.global.dto.UserSummary;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchingSessionUserAdapter implements LoadUserPort {

  private final UserService userService;

  @Override
  public UserSummary getUserSummary(UUID userId) {
    var userDto = userService.find(userId);
    return toSummary(userDto);
  }

  @Override
  public Map<UUID, UserSummary> getUserSummaries(Collection<UUID> userIds) {
    return userService.findAllByIds(userIds).stream()
        .collect(Collectors.toMap(UserDto::id, WatchingSessionUserAdapter::toSummary));
  }

  private static UserSummary toSummary(UserDto userDto) {
    return new UserSummary(userDto.id(), userDto.name(), userDto.profileImageUrl());
  }
}