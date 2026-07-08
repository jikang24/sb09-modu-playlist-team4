package com.mopl.domain.dm.domain;


import com.mopl.global.exception.MoplException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DirectMessageTest {
  private final UUID conversationId = UUID.randomUUID();
  private final UUID senderId = UUID.randomUUID();
  private final UUID receiverId = UUID.randomUUID();
  private final String content = "test description";

  @Test
  @DisplayName("DM 생성 성공")
  void create_success(){
    DirectMessage dm = DirectMessage.create(conversationId, senderId, receiverId, content);
    assertThat(dm.getId()).isNotNull();
    assertThat(dm.getConversationId()).isEqualTo(conversationId);
    assertThat(dm.getSenderId()).isEqualTo(senderId);
    assertThat(dm.getReceiverId()).isEqualTo(receiverId);
    assertThat(dm.getContent()).isEqualTo(content);
    assertThat(dm.getCreatedAt()).isNotNull();
    assertThat(dm.isRead()).isFalse();

  }

  @Test
  @DisplayName("DM 생성 실패 - 내용이 빈 문자열")
  void create_fail_emptyContent(){
    assertThatThrownBy(() -> DirectMessage.create(conversationId, senderId, receiverId, ""))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("DM 생성 실패- 내용이 null")
  void create_fail_nullContent(){
    assertThatThrownBy(() -> DirectMessage.create(conversationId, senderId, receiverId, null))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("DM 읽음 처리 - 초기 값은 false")
  void isRead_initial_false(){
    DirectMessage dm = DirectMessage.create(conversationId, senderId, receiverId, content);
    assertThat(dm.isRead()).isFalse();
  }

  @Test
  @DisplayName("DM 읽음 처리 성공")
  void markAsRead_success(){
    DirectMessage dm = DirectMessage.create(conversationId, senderId, receiverId, content);
    dm.markAsRead();
    assertThat(dm.isRead()).isTrue();
  }

  @Test
  @DisplayName("보낸 사람 확인 - 본인이 발신자인 경우")
  void isSender_true(){
    DirectMessage dm = DirectMessage.create(conversationId, senderId, receiverId, content);
    assertThat(dm.isSender(senderId)).isTrue();
  }

  @Test
  @DisplayName("보낸 사람 확인 - 본인이 발신자가 아닌 경우")
  void isSender_false() {
    DirectMessage dm = DirectMessage.create(conversationId, senderId, receiverId, "안녕");

    assertThat(dm.isSender(receiverId)).isFalse();
  }

  @Test
  @DisplayName("보낸 사람 확인 - 전혀 다른 사람인 경우")
  void isSender_other_user() {

    DirectMessage dm = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    UUID otherId = UUID.randomUUID();


    assertThat(dm.isSender(otherId)).isFalse();
  }

}