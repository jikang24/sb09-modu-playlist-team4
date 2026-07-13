package com.mopl.domain.conversation.adapter.out.user;


import com.mopl.domain.conversation.application.port.out.LoadUserPort;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.dto.UserSearchRequest;
import com.mopl.domain.user.dto.UserSortBy;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.response.CursorPageResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationUserAdapter implements LoadUserPort {

private final UserService userService;

  @Override
  @Cacheable(value = "userSummary", key = "#userId")
  public UserSummary getUserSummary(UUID userId) {
    var userDto = userService.find(userId);
    return new UserSummary(
        userDto.id(),
        userDto.name(),
        userDto.profileImageUrl()
    );
  }

  @Override
  public List<UUID> findUserIdsByNameLike(String keyword) {
    UserSearchRequest request = new UserSearchRequest(
        keyword,
        null,
        null,
        null,
        null,
        null,
        100,
        SortDirection.ASCENDING,
        UserSortBy.NAME
    );

    CursorPageResponse<UserDto> result = userService.findAll(request);

    return result.data().stream()
        .map(UserDto::id)
        .toList();
  }
}
