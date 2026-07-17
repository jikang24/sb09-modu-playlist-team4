package com.mopl.domain.dm.adapter.out.user;

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
class DmUserAdapterTest {

  @InjectMocks
  private DmUserAdapter dmUserAdapter;

  @Mock
  private UserService userService;

  @Test
  @DisplayName("userId로 UserSummary 조회 성공")
  void getUserSummary_success() {
    
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(
        userId, Instant.now(), "test@email.com", "test", "profile.jpg", Role.USER, false
    );
    given(userService.find(userId)).willReturn(userDto);

    
    UserSummary result = dmUserAdapter.getUserSummary(userId);

    
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.name()).isEqualTo("test");
    assertThat(result.profileImageUrl()).isEqualTo("profile.jpg");
  }

  @Test
  @DisplayName("profileImageUrl이 null인 경우도 정상 매핑")
  void getUserSummary_success_nullProfileImage() {
    
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(
        userId, Instant.now(), "test@email.com", "test", null, Role.USER, false
    );
    given(userService.find(userId)).willReturn(userDto);

    
    UserSummary result = dmUserAdapter.getUserSummary(userId);

    
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.name()).isEqualTo("test");
    assertThat(result.profileImageUrl()).isNull();
  }

  @Test
  @DisplayName("userId 목록으로 UserSummary를 한 번에 조회한다")
  void getUserSummaries_success() {

    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    UserDto userDto1 = new UserDto(
        userId1, Instant.now(), "a@email.com", "userA", "a.jpg", Role.USER, false
    );
    UserDto userDto2 = new UserDto(
        userId2, Instant.now(), "b@email.com", "userB", null, Role.USER, false
    );
    given(userService.findAllByIds(List.of(userId1, userId2)))
        .willReturn(List.of(userDto1, userDto2));


    Map<UUID, UserSummary> result = dmUserAdapter.getUserSummaries(List.of(userId1, userId2));


    assertThat(result).hasSize(2);
    assertThat(result.get(userId1).name()).isEqualTo("userA");
    assertThat(result.get(userId2).name()).isEqualTo("userB");
    assertThat(result.get(userId2).profileImageUrl()).isNull();
  }

  @Test
  @DisplayName("userId 목록이 비어있으면 빈 Map을 반환한다")
  void getUserSummaries_empty() {

    given(userService.findAllByIds(List.of())).willReturn(List.of());


    Map<UUID, UserSummary> result = dmUserAdapter.getUserSummaries(List.of());


    assertThat(result).isEmpty();
  }
}