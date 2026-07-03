package com.mopl.domain.conversation.domain;

import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class ConversationTest {

  private final UUID userId1 = UUID.randomUUID();
  private final UUID userId2 = UUID.randomUUID();

  @Test
  @DisplayName("대화방 생성 성공")
  void create_success() {
    
    Conversation conversation = Conversation.create(userId1, userId2);

    
    assertThat(conversation.getId()).isNotNull();
    assertThat(conversation.getParticipant1Id()).isEqualTo(userId1);
    assertThat(conversation.getParticipant2Id()).isEqualTo(userId2);
    assertThat(conversation.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("대화방 생성 실패 - 자기 자신과 대화")
  void create_fail_same_user() {
    
    assertThatThrownBy(() -> Conversation.create(userId1, userId1))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("상대방 ID 조회 - participant1이 요청한 경우")
  void getOtherParticipant_from_participant1() {
    
    Conversation conversation = Conversation.create(userId1, userId2);

    
    UUID otherParticipant = conversation.getOtherParticipant(userId1);

    
    assertThat(otherParticipant).isEqualTo(userId2);
  }

  @Test
  @DisplayName("상대방 ID 조회 - participant2가 요청한 경우")
  void getOtherParticipant_from_participant2() {
    
    Conversation conversation = Conversation.create(userId1, userId2);

    
    UUID otherParticipant = conversation.getOtherParticipant(userId2);

    
    assertThat(otherParticipant).isEqualTo(userId1);
  }

  @Test
  @DisplayName("상대방 ID 조회 실패 - 참여자가 아닌 경우")
  void getOtherParticipant_fail_not_participant() {
    
    Conversation conversation = Conversation.create(userId1, userId2);
    UUID otherId = UUID.randomUUID();

    
    assertThatThrownBy(() -> conversation.getOtherParticipant(otherId))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("참여자 확인 - participant1인 경우")
  void hasParticipant_true_participant1() {
    
    Conversation conversation = Conversation.create(userId1, userId2);

    
    assertThat(conversation.hasParticipant(userId1)).isTrue();
  }

  @Test
  @DisplayName("참여자 확인 - participant2인 경우")
  void hasParticipant_true_participant2() {
    
    Conversation conversation = Conversation.create(userId1, userId2);

    
    assertThat(conversation.hasParticipant(userId2)).isTrue();
  }

  @Test
  @DisplayName("참여자 확인 - 참여자가 아닌 경우")
  void hasParticipant_false() {
    
    Conversation conversation = Conversation.create(userId1, userId2);
    UUID otherId = UUID.randomUUID();

    
    assertThat(conversation.hasParticipant(otherId)).isFalse();
  }

  @Test
  @DisplayName("참여자 목록 조회")
  void getParticipants() {
    
    Conversation conversation = Conversation.create(userId1, userId2);

    
    assertThat(conversation.getParticipants())
        .containsExactlyInAnyOrder(userId1, userId2);
  }
}