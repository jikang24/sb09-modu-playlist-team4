package com.mopl.domain.conversation.adapter.out.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.dto.UserSearchRequest;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.response.CursorPageResponse;
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
class ConversationUserAdapterTest {

  @InjectMocks
  private ConversationUserAdapter conversationUserAdapter;
  @Mock
  private UserService userService;

  @Test
  @DisplayName("userId로 UserSummary 조회 성공")
  void getUserSummary_success(){
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(
        userId,
        Instant.now(),
        "test@email.com",
        "test",
        "profile.jpg",
        Role.USER,
        false
    );

    given(userService.find(userId)).willReturn(userDto);

    UserSummary result = conversationUserAdapter.getUserSummary(userId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.name()).isEqualTo("test");
    assertThat(result.profileImageUrl()).isEqualTo("profile.jpg");
  }

  @Test
  @DisplayName("userId 목록으로 UserSummary를 한 번에 조회한다")
  void getUserSummaries_success(){
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    UserDto user1 = new UserDto(userId1, Instant.now(), "a@email.com", "userA", "a.jpg", Role.USER, false);
    UserDto user2 = new UserDto(userId2, Instant.now(), "b@email.com", "userB", null, Role.USER, false);

    given(userService.findAllByIds(List.of(userId1, userId2))).willReturn(List.of(user1, user2));

    Map<UUID, UserSummary> result = conversationUserAdapter.getUserSummaries(List.of(userId1, userId2));

    assertThat(result).hasSize(2);
    assertThat(result.get(userId1).name()).isEqualTo("userA");
    assertThat(result.get(userId2).name()).isEqualTo("userB");
  }

  @Test
  @DisplayName("키워드로 이름 검색 시 userId 목록 반환")
  void findUserIdsByNameLike_success(){
    String keyword = "test";
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    UserDto user1 = new UserDto(userId1,Instant.now(),"test@naver.com","test1",null,Role.USER,
        false);
    UserDto user2 = new UserDto(userId2 , Instant.now(),"test2@naver.com","test2",null,Role.USER,false);
    CursorPageResponse<UserDto> response = new CursorPageResponse<>(
        List.of(user1, user2), null, null, false, 2, "NAME", "ASCENDING"
    );
    given(userService.findAll(any(UserSearchRequest.class))).willReturn(response);

    List<UUID> result = conversationUserAdapter.findUserIdsByNameLike(keyword);
    assertThat(result).containsExactly(userId1, userId2);
    assertThat(result).hasSize(2);

  }

  @Test
  @DisplayName("키워드로 이름 검색 시 결과 없으면 빈 리스트 반환")
  void findUserIdsByNameLike_empty(){
    String keyword = "unknown";
    CursorPageResponse<UserDto> response = new CursorPageResponse<>(
        List.of(), null, null, false, 0, "NAME", "ASCENDING"
    );
    given(userService.findAll(any(UserSearchRequest.class))).willReturn(response);
    List<UUID> result = conversationUserAdapter.findUserIdsByNameLike(keyword);
    assertThat(result).isEmpty();

  }

}