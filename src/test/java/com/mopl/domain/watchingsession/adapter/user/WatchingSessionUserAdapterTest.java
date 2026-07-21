package com.mopl.domain.watchingsession.adapter.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingSessionUserAdapterTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private WatchingSessionUserAdapter adapter;

  private UserDto makeUserDto(UUID id, String name) {
    return new UserDto(id, Instant.now(), id + "@test.com", name, "https://profile.jpg", Role.USER, false);
  }

  @Test
  @DisplayName("getUserSummary: UserService 조회 결과를 UserSummary로 변환해서 반환한다")
  void getUserSummary_mapsUserDtoToSummary() {
    UUID userId = UUID.randomUUID();
    given(userService.find(userId)).willReturn(makeUserDto(userId, "홍길동"));

    UserSummary summary = adapter.getUserSummary(userId);

    assertThat(summary).isEqualTo(new UserSummary(userId, "홍길동", "https://profile.jpg"));
  }

  @Test
  @DisplayName("getUserSummaries: 여러 유저를 한 번에 조회해서 id 기준 Map으로 변환한다")
  void getUserSummaries_mapsBulkResultById() {
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    List<UUID> userIds = List.of(userId1, userId2);
    given(userService.findAllByIds(userIds)).willReturn(List.of(
        makeUserDto(userId1, "유저1"),
        makeUserDto(userId2, "유저2")
    ));

    Map<UUID, UserSummary> summaries = adapter.getUserSummaries(userIds);

    assertThat(summaries)
        .containsEntry(userId1, new UserSummary(userId1, "유저1", "https://profile.jpg"))
        .containsEntry(userId2, new UserSummary(userId2, "유저2", "https://profile.jpg"));
  }

  @Test
  @DisplayName("getUserSummaries: 조회 대상이 없으면 빈 Map을 반환한다")
  void getUserSummaries_empty() {
    given(userService.findAllByIds(List.of())).willReturn(List.of());

    Map<UUID, UserSummary> summaries = adapter.getUserSummaries(List.of());

    assertThat(summaries).isEmpty();
  }
}