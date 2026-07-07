package com.mopl.domain.dm.adapter.out.user;

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
}