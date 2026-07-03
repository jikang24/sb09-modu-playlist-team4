package com.mopl.domain.conversation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.application.port.out.LoadConversationPort;
import com.mopl.domain.conversation.application.port.out.SaveConversationPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

  @InjectMocks
  private ConversationService conversationService;

  @Mock
  private SaveConversationPort saveConversationPort;

  @Mock
  private LoadConversationPort loadConversationPort;

  private final UUID userId1 = UUID.randomUUID();
  private final UUID userId2 = UUID.randomUUID();

  @Test
  @DisplayName("대화방 생성 성공")
  void create_success() {
    
    Conversation conversation = new Conversation(
        UUID.randomUUID(), userId1, userId2, Instant.now()
    );
    given(saveConversationPort.save(any(Conversation.class))).willReturn(conversation);

    
    Conversation result = conversationService.create(userId1, userId2);

    
    assertThat(result).isNotNull();
    assertThat(result.hasParticipant(userId1)).isTrue();
    assertThat(result.hasParticipant(userId2)).isTrue();
    then(saveConversationPort).should().save(any(Conversation.class));
  }

  @Test
  @DisplayName("대화방 생성 실패 - 자기 자신과 대화")
  void create_fail_same_user() {
    
    assertThatThrownBy(() -> conversationService.create(userId1, userId1))
        .isInstanceOf(MoplException.class);
    then(saveConversationPort).should(never()).save(any());
  }

  @Test
  @DisplayName("대화방 단건 조회 성공")
  void getById_success() {
    
    UUID conversationId = UUID.randomUUID();
    Conversation conversation = new Conversation(
        conversationId, userId1, userId2, Instant.now()
    );
    given(loadConversationPort.findById(conversationId)).willReturn(Optional.of(conversation));

    
    Conversation result = conversationService.getById(conversationId, userId1);

    
    assertThat(result).isNotNull();
    assertThat(result.hasParticipant(userId1)).isTrue();
    then(loadConversationPort).should().findById(conversationId);
  }

  @Test
  @DisplayName("대화방 단건 조회 실패 - 존재하지 않는 대화방")
  void getById_fail_not_found() {
    
    UUID conversationId = UUID.randomUUID();
    given(loadConversationPort.findById(conversationId)).willReturn(Optional.empty());

    
    assertThatThrownBy(() -> conversationService.getById(conversationId, userId1))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("대화방 단건 조회 실패 - 참여자가 아닌 경우")
  void getById_fail_not_participant() {
    
    UUID conversationId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    Conversation conversation = new Conversation(
        conversationId, userId1, userId2, Instant.now()
    );
    given(loadConversationPort.findById(conversationId)).willReturn(Optional.of(conversation));

    
    assertThatThrownBy(() -> conversationService.getById(conversationId, otherId))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("특정 사용자와의 대화방 조회 성공")
  void getByParticipant_success() {
    
    Conversation conversation = new Conversation(
        UUID.randomUUID(), userId1, userId2, Instant.now()
    );
    given(loadConversationPort.findByParticipants(userId1, userId2))
        .willReturn(Optional.of(conversation));

    
    Conversation result = conversationService.getByParticipant(userId1, userId2);

    
    assertThat(result).isNotNull();
    assertThat(result.hasParticipant(userId1)).isTrue();
    assertThat(result.hasParticipant(userId2)).isTrue();
    then(loadConversationPort).should().findByParticipants(userId1, userId2);
  }

  @Test
  @DisplayName("특정 사용자와의 대화방 조회 실패 - 존재하지 않는 대화방")
  void getByParticipant_fail_not_found() {
    
    given(loadConversationPort.findByParticipants(userId1, userId2))
        .willReturn(Optional.empty());

    
    assertThatThrownBy(() -> conversationService.getByParticipant(userId1, userId2))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("대화방 목록 조회 성공")
  void getList_success() {
    
    ConversationSearchCondition condition = new ConversationSearchCondition(
        null, null, null, 10, "createdAt", SortDirection.ASCENDING
    );
    Conversation conversation = new Conversation(
        UUID.randomUUID(), userId1, userId2, Instant.now()
    );
    CursorPageResponse<Conversation> response = new CursorPageResponse<>(
        List.of(conversation), null, null, false, 1, "createdAt", "ASCENDING"
    );
    given(loadConversationPort.findList(userId1, condition)).willReturn(response);

    
    CursorPageResponse<Conversation> result = conversationService.getList(userId1, condition);

    
    assertThat(result).isNotNull();
    assertThat(result.data()).hasSize(1);
    then(loadConversationPort).should().findList(userId1, condition);
  }

  @Test
  @DisplayName("대화방 목록 조회 - 빈 결과")
  void getList_empty() {
    
    ConversationSearchCondition condition = new ConversationSearchCondition(
        null, null, null, 10, "createdAt", SortDirection.ASCENDING
    );
    CursorPageResponse<Conversation> response = new CursorPageResponse<>(
        List.of(), null, null, false, 0, "createdAt", "ASCENDING"
    );
    given(loadConversationPort.findList(userId1, condition)).willReturn(response);

    
    CursorPageResponse<Conversation> result = conversationService.getList(userId1, condition);

    
    assertThat(result).isNotNull();
    assertThat(result.data()).isEmpty();
  }
}