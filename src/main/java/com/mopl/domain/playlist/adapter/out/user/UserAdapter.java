package com.mopl.domain.playlist.adapter.out.user;

import com.mopl.domain.playlist.application.port.out.LoadUserPort;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.UserSummary;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("playlistUserAdapter")
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPort {

  private final UserService userService;

  @Override
  public UserSummary getUserSummary(UUID userId) {
    var userDto = userService.find(userId);
    return new UserSummary(
        userDto.id(),
        userDto.name(),
        userDto.profileImageUrl()
    );
  }

  @Override
  public Map<UUID, UserSummary> getUserSummaries(Collection<UUID> userIds) {
    return userService.findAllByIds(userIds).stream()
        .collect(Collectors.toMap(
            UserDto::id,
            UserAdapter::toSummary
        ));
  }

  private static UserSummary toSummary(UserDto userDto) {
    return new UserSummary(
        userDto.id(),
        userDto.name(),
        userDto.profileImageUrl()
    );
  }
}
