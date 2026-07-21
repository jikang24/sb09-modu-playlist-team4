package com.mopl.domain.review.adapter.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewUserAdapterTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private ReviewUserAdapter adapter;

  @Test
  @DisplayName("getUserSummary: findRaw() 조회 결과(presign 미적용 원본 값)를 UserSummary로 변환해 반환한다")
  void getUserSummary_mapsUserDtoToUserSummary() {
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(
        userId, Instant.now(), "user@email.com", "닉네임", "profile-key.jpg",
        Role.USER, false);
    given(userService.findRaw(userId)).willReturn(userDto);

    UserSummary result = adapter.getUserSummary(userId);

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.name()).isEqualTo("닉네임");
    assertThat(result.profileImageUrl()).isEqualTo("profile-key.jpg");
  }
}